// Copyright (c) 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package org.books;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.books.bobby.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.propagation.Format.Builtin;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import static org.books.utils.TracingUtils.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

@Named
@SessionScoped
public class BookBean implements Serializable {

	private static Logger logger = LoggerFactory.getLogger(BookBean.class);

    private Book book;

    private String bookId;

	@Inject
	private HttpServletRequest servletRequest;

    public void find() {
		Span tracingSpan = buildSpan("BookBean.find", servletRequest);
		Scope tracingScope = tracerPreprocessing(tracingSpan);

		logger.info("In find(), with bookId=" + bookId);

        if (bookId == null) {
            bookId = "2";
        }

        try  {
            InitialContext ctx = new InitialContext();
            DataSource booksDS = (DataSource) ctx.lookup("jdbc/books");

            String sql = "select book_id, authors, title, image_url from books where book_id = " + bookId;

            logger.info("Executing request " + sql);

            try (Connection connection = booksDS.getConnection();
                 Statement statement = connection.createStatement();

                 ResultSet resultSet =
                         statement.executeQuery(sql);) {
                tracingScope = startTracing(tracingSpan, connection);

                while (resultSet.next()) {



                    book = new Book();
                    book.setBookId(resultSet.getString("book_id"));
                    book.setAuthors(resultSet.getString("authors"));
                    book.setTitle(resultSet.getString("title"));
                    book.setImageUrl(resultSet.getString("image_url"));


                }
            }



        } catch (Exception e) {
            logger.error("Error fetching books\n", e);
        }

        if (book != null) {
			tracer().activeSpan().log(String.format("Found book: %s", book.toString()));
		}

		logger.info("Got book:" + book.toString());
		finishTrace(tracingScope);
    }

	private Scope tracerPreprocessing(Span tracingSpan) {
		tracingSpan.setTag("bookId", bookId);
		return activateSpan(tracingSpan);

	}

    private Scope startTracing(Span tracingSpan, Connection connection) throws SQLException {
        tracingSpan.setTag("TimeSpentInDBOperationFor_" + connection, "TODO");
        tracingSpan.setBaggageItem("DatabaseProductName", connection.getMetaData().getDatabaseProductName());
        return activateSpan(tracingSpan);
    }

	public String getTitle() {
        return book.getTitle();
    }

    public String getAuthors() {
        return book.getAuthors();
    }

    public String getImageUrl() {
        return book.getImageUrl();
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
}
