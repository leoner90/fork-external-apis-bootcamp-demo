package com.accenture.externalapis.demo.client;

// TODO: Design this exception yourself. pre done
// It should extend RuntimeException and provide at least a constructor that
// takes a message, and one that takes a message + cause (for wrapping the
// original RestClient/WebClient exception).
public class ClientException extends RuntimeException
{
    public ClientException(String msg)
    {
        super(msg);
    }

    public ClientException(String msg, Throwable reason)
    {
        super(msg, reason);
    }
}
