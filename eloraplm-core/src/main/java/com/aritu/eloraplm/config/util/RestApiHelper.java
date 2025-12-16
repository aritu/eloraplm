/*
 * (C) Copyright 2015 Aritu S Coop (http://aritu.com/).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package com.aritu.eloraplm.config.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.aritu.eloraplm.exceptions.EloraException;

/**
 * // TODO: write class general comment
 *
 * @author aritu
 *
 */

public class RestApiHelper {

    public static String get(String uri, String params)
            throws EloraException, IOException {

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        String result = "";

        try {

            HttpGet httpGet = new HttpGet(uri + params);

            HttpResponse response = httpClient.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new EloraException(
                        "Failed with HTTP error code : " + statusCode);
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity);
            }

            return result;

        } catch (Exception e) {
            String errorMsg = "Error calling to REST API. errorMsg = |"
                    + e.getMessage() + "|";

            throw new EloraException(errorMsg);

        } finally {
            httpClient.close();
        }
    }

    public static String post(String uri, String json)
            throws EloraException, IOException {

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        String result = "";

        try {

            HttpPost httpPost = new HttpPost(uri);

            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("content-type", "application/json");

            StringEntity postEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(postEntity);

            HttpResponse response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 201) {
                throw new EloraException(
                        "Failed with HTTP error code : " + statusCode);
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity);
            }

            return result;

        } catch (Exception e) {
            String errorMsg = "Error calling to REST API. errorMsg = |"
                    + e.getMessage() + "|";

            throw new EloraException(errorMsg);

        } finally {
            httpClient.close();
        }
    }

}
