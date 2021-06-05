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
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.*;
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
import java.util.List;

@Named
@SessionScoped
public class BooksBean implements Serializable {

	private static Logger logger = LoggerFactory.getLogger(BooksBean.class);

    private List<Book> books;

    private String start;
    private String end;

	@Inject
	private HttpServletRequest servletRequest;

    public void find() {
		Span tracingSpan = buildSpan("BooksBean.find", servletRequest);
		Scope tracingScope = tracerPreprocessing(tracingSpan);

		logger.info("In find(), with start=" + start + " and end=" + end);

        if (start == null) {
            start = "1";
        }

        if (end == null) {
            end = "13";
        }

        try  {
            InitialContext ctx = new InitialContext();
            DataSource booksDS = (DataSource) ctx.lookup("jdbc/books");

            String sql = "select book_id, authors, title, image_url from books limit " + start + ", " + end;

            logger.info("Executing request " + sql);

            try (Connection connection = booksDS.getConnection();
                 Statement statement = connection.createStatement();

                 ResultSet resultSet =
                         statement.executeQuery(sql);) {
                tracingScope = startTracing(tracingSpan, connection);
                books = new ArrayList<Book>();
                while (resultSet.next()) {



                    Book book = new Book();
                    book.setBookId(resultSet.getString("book_id"));
                    book.setAuthors(resultSet.getString("authors"));
                    book.setTitle(resultSet.getString("title"));
                    book.setImageUrl(resultSet.getString("image_url"));
                    books.add(book);

                }
            }



        } catch (Exception e) {
			logger.error("Error fetching books\n", e);
        }
		if (books != null) {
			tracer().activeSpan().log(String.format("Found %d books", books.size()));
		}
		finishTrace(tracingScope);

    }

	private Scope tracerPreprocessing(Span tracingSpan) {
		tracingSpan.setTag("start", start);
		tracingSpan.setTag("end", end);
		return activateSpan(tracingSpan);

	}

    private Scope startTracing(Span tracingSpan, Connection connection) throws SQLException {
        tracingSpan.setTag("TimeSpentInDBOperationFor_" + connection, "TODO");
        tracingSpan.setBaggageItem("DatabaseProductName", connection.getMetaData().getDatabaseProductName());
        return activateSpan(tracingSpan);
    }

	public void previous() {
        int s = Integer.parseInt(getStart()) - 12;
        if (s < 0) s = 0;

        int e = s + 12;
        setStart(Integer.toString(s));
        setEnd(Integer.toString(e));
		logger.info("previous: s=" + getStart() + " e=" + getEnd());
        reload();
    }

    public void next() {
        int s = Integer.parseInt(getStart()) + 12;
        int e = s + 12;
        setStart(Integer.toString(s));
        setEnd(Integer.toString(e));
		logger.info("next: s=" + getStart() + " e=" + getEnd());
        reload();
    }

    public List<Book> getBooks() {
        return books;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    private void reload() {
        try {
            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
            ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());
        } catch (Exception e) {
			logger.error("failed to refresh page", e);
        }
    }

}
