package com.camptocamp.sbb;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Function;

public class GeoserverRest {
	public static final String GEOSERVER_URL = "http://localhost:8081/geoserver/";

	public static <T> T execRestDom(String restRequest, final Function<Document, T> func) {
		return execRestInputStream(restRequest, new Function<InputStream, T>() {

			@Override
			public T apply(InputStream in) {

				try {
					DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					InputSource is = new InputSource();
					is.setByteStream(in);
					Document doc = db.parse(is);
					return func.apply(doc);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		});
	}

	public static <T> T execRestInputStream(String restRequest, Function<InputStream, T> func) {
		try {
			URL url = new URL(restRequest);
			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			try {
				String authString = "admin:geoserver";
				String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes());
				openConnection.addRequestProperty("Authorization", "Basic " + authStringEnc);

				return func.apply(openConnection.getInputStream());
			} finally {
				openConnection.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static int post(String restRequest, String method, String contentType, String sld) {
		try {
			URL url = new URL(restRequest);
			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			try {
				openConnection.setDoOutput(true);
				openConnection.setRequestMethod(method);

				String authString = "admin:geoserver";
				String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes());
				openConnection.addRequestProperty("Authorization", "Basic " + authStringEnc);
				openConnection.addRequestProperty("Content-type", contentType);

				OutputStream outputStream = openConnection.getOutputStream();
				IOUtils.write(sld, outputStream);
				outputStream.close();
				
				return openConnection.getResponseCode();
			} finally {
				openConnection.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 500;
		}
	}

}
