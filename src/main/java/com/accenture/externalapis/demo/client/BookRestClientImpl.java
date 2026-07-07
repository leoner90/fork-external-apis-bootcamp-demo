package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import org.springframework.web.client.*; // probably should be only required lines

import java.util.List;
import java.util.Arrays;


// implemented interface BookRestClient
@Component
public class BookRestClientImpl implements BookRestClient
{
    private final RestClient restClient;


    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties)
    {
        //Constructor Build the RestClient using builder.baseUrl(properties.baseUrl()).build()
        //+ fake token sent for Authorization
        String token = "Fake_Token12345";
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }


    // Interface Override - Implement getBook(Long id) - fetch one book from GET /books/{id} as a + Handle the main RestClient error cases and rethrow them as ClientException:
    @Override
    public BookDto getBook(Long id)
    {
        try
        {
            BookApiResponse response = restClient
                    .get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);

            //response could be null
            if (response == null)
            {
                throw new ClientException("External service returned an empty response for book " + id); // as returning null is not properly handled in controller, also probably not best idea return id but make log and return generic line!!!
            }

            // BookApiResponse, then map it onto a BookDto (only keep the fields BookDto needs).
            return new BookDto(response.title(), response.author(), response.genre(), response.price());

        }
        // ERRORS Handle the main RestClient error cases and rethrow them as ClientException:HttpClientErrorException (4xx, e.g. book not found)HttpServerErrorException (5xx, e.g. the faulty/teapot book) ResourceAccessException (connection refused / timeout - the external service is unreachable)
        catch (HttpClientErrorException exception)
        {
            throw new ClientException("Client error while fetching book " + id, exception);
        }
        catch (HttpServerErrorException exception)
        {
            throw new ClientException("Server error while fetching book " + id, exception);
        }
        catch (ResourceAccessException exception)
        {
            throw new ClientException("External book service is unavailable", exception);
        }
    }


    // Implement getAllBooks() - fetch all books from GET /books as + Handle the main RestClient error cases and rethrow them as ClientException:
    @Override
    public List<BookDto> getAllBooks()
    {
        try
        {
            //get books
            BookApiResponse[] response = restClient
                    .get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            //response could be null
            if (response == null)
            {
                return List.of();
            }

            //BookApiResponse[], then map each one onto a BookDto. Handle the same error
            return Arrays.stream(response)
                    .map(book -> new BookDto(book.title(), book.author(), book.genre(), book.price()))
                    .toList();
        }
        // ERRORS
        catch (HttpClientErrorException exception)
        {
            throw new ClientException("Client error while fetching all books", exception);
        }
        catch (HttpServerErrorException exception)
        {
            throw new ClientException("Server error while fetching all books", exception);
        }
        catch (ResourceAccessException exception)
        {
            throw new ClientException("External book service is unavailable", exception);
        }
    }
}
