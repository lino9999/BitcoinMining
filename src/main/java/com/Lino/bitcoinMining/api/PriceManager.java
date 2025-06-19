package com.Lino.bitcoinMining.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.Lino.bitcoinMining.BitcoinMining;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PriceManager {

    private final BitcoinMining plugin;
    private double currentPrice;
    private double previousPrice;
    private final List<PriceData> priceHistory;
    private static final String API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";

    public PriceManager(BitcoinMining plugin) {
        this.plugin = plugin;
        this.currentPrice = 50000.0;
        this.previousPrice = 50000.0;
        this.priceHistory = new ArrayList<>();
        updatePrice();
    }

    public void updatePrice() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                double newPrice = json.getAsJsonObject("bitcoin").get("usd").getAsDouble();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    previousPrice = currentPrice;
                    currentPrice = newPrice;
                    priceHistory.add(new PriceData(System.currentTimeMillis(), newPrice));

                    if (priceHistory.size() > 288) {
                        priceHistory.remove(0);
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to fetch Bitcoin price: " + e.getMessage());
            }
        });
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getPreviousPrice() {
        return previousPrice;
    }

    public double getPriceChange() {
        return currentPrice - previousPrice;
    }

    public double getPriceChangePercentage() {
        if (previousPrice == 0) return 0;
        return ((currentPrice - previousPrice) / previousPrice) * 100;
    }

    public List<PriceData> getPriceHistory() {
        return new ArrayList<>(priceHistory);
    }

    public static class PriceData {
        private final long timestamp;
        private final double price;

        public PriceData(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }

        public long getTimestamp() { return timestamp; }
        public double getPrice() { return price; }
    }
}