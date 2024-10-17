package com.escanerentrada.camera.barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Clase que se encarga de obtener la información de la API.
 */
public class BarcodeAnalyzer {

    private static final OkHttpClient client =  new OkHttpClient();

    /**
     * Método que obtiene la información de la API.
     *
     * @param barcode Código de barras
     * @param info Información
     * @return Información
     */
    public String getInfo(String barcode, String info) {
        String url = "https://opengtindb.org/?ean=" + barcode + "&cmd=query&queryid=" + Math.random();
        String result = "";

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            switch (info) {
                case "detail":
                case "brand":
                case "manufacturer":
                    if (json.has("detail")) {
                        JSONObject detail = json.getJSONObject("detail");
                        result = detail.optString("brand", detail.optString("manufacturer", "Empresa desconocida"));
                    } else if (json.has("product")) {
                        JSONObject product = json.getJSONObject("product");
                        result = product.optString("brand", product.optString("manufacturer", "Empresa desconocida"));
                    } else {
                        result = "Empresa desconocida";
                    }
                    break;
                case "name":
                    if (json.has("detail")) {
                        JSONObject detail = json.getJSONObject("detail");
                        result = detail.optString("name", "Producto desconocido");
                    } else if (json.has("product")) {
                        JSONObject product = json.getJSONObject("product");
                        result = product.optString("name", "Producto desconocido");
                    } else {
                        result = "Producto desconocido";
                    }
                    break;
                case "origin":
                    if (json.has("detail")) {
                        JSONObject detail = json.getJSONObject("detail");
                        result = detail.optString("origin", "Origen desconocido");
                    } else if (json.has("product")) {
                        JSONObject product = json.getJSONObject("product");
                        result = product.optString("origin", "Origen desconocido");
                    } else {
                        result = "Origen desconocido";
                    }
                    break;
            }

        } catch (IOException e) {
            result = "Error de red";
        } catch (JSONException e) {
            result = "Error de JSON";
        } catch (Exception e) {
            result = "No info";
        }

        return result;
    }
}
