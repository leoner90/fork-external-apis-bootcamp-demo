package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.Collections;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

//for the bonus task
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import reactor.util.retry.Retry;

import java.util.List;


 //interface implemented
@Component
public class BookWebClientImpl implements  BookWebClient
{
    private final WebClient webClient;

    //webClient builder
    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties)
    {
        String token = "Fake_Token12345";

        // Build the WebClient using builder.baseUrl(properties.baseUrl()).build()
        //+ fake token sent for Authorization
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }


    //Implement getBookAsync(Long id) - fetch one book from GET /books/{id}  Mono<BookApiResponse>, then map it onto a Mono<BookDto>.
    @Override
    public Mono<BookDto> getBookAsync(Long id)
    {
        return webClient
            .get()
            .uri("/books/{id}", id)
            .retrieve()
            .bodyToMono(BookApiResponse.class)
            .map(book -> new BookDto(
                    book.title(),
                    book.author(),
                    book.genre(),
                    book.price()
            ))
            .timeout(Duration.ofSeconds(5))
            .retryWhen
            (
                Retry.backoff(2, Duration.ofMillis(500)).filter(this::isRetryable)
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> retrySignal.failure())
            )

            //errors Handler ( guess it should not be one line( will avoid in the future, rn is a bit easier to understand)
            .onErrorMap
            (
                TimeoutException.class, exception -> new ClientException("External service timed out while fetching book " + id, exception)
            )
            .onErrorMap
            (
                WebClientResponseException.class, exception -> new ClientException("External service returned status " + exception.getStatusCode() + " while fetching book " + id, exception)
            )
            .onErrorMap
            (
                WebClientRequestException.class, exception -> new ClientException("Could not connect to the external service while fetching book " + id, exception)
            )
            .onErrorMap
            (
                //just extra error Check like 991 book -> for any other
                exception -> !(exception instanceof ClientException),
                exception -> new ClientException("Invalid or unexpected response while fetching book like book(991)" + id, exception)
            );
    }


    //Implement getAllBooksAsync() - fetch all books from GET /books as + errors handler Flux<BookApiResponse>,  and map into BookDto
    @Override
    public Flux<BookDto> getAllBooksAsync()
    {
        return webClient
                .get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(book -> new BookDto(
                        book.title(),
                        book.author(),
                        book.genre(),
                        book.price()
                ))

                //errors Handler
                .onErrorMap
                (
                    WebClientResponseException.class,
                    exception -> new ClientException("External service returned status " + exception.getStatusCode() + " while fetching all books", exception)
                )
                .onErrorMap
                (
                    WebClientRequestException.class, exception -> new ClientException("Could not connect to the external service while fetching all books", exception)
                );
    }


    // Implement getBooksInParallel(Long id1, Long id2) - fetch two books in parallel with Mono.zip(). Handle the same error cases as getBookAsync() above.
    //could be rewrote to get result regardless of how many books , but return order would be based on finish time
    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2)
    {
        Mono<BookDto> firstBook = getBookAsync(id1);
        Mono<BookDto> secondBook = getBookAsync(id2);

        return Mono.zip(firstBook, secondBook)
                .map(result -> List.of(result.getT1(), result.getT2()));
    }


    // Got some little smart bot help, as  I would pass true to retry instead,  so here Retryable Errors for retry
    private boolean isRetryable(Throwable exception)
    {
        if (exception instanceof TimeoutException)
        {
            return true;
        }

        if (exception instanceof WebClientRequestException)
        {
            return true;
        }

        if (exception instanceof WebClientResponseException responseException)
        {
            return responseException.getStatusCode().is5xxServerError();
        }

        return false;
    }

}
