
package com.crio.warmup.stock.quotes;


import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

 

  private RestTemplate restTemplate;

  public AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException {
 
    String responseString = restTemplate.getForObject(buildURL(symbol), String.class);

    AlphavantageDailyResponse alphavantageDailyResponse;
    try {
      alphavantageDailyResponse =
          getObjectMapper().readValue(responseString, AlphavantageDailyResponse.class);
      if (alphavantageDailyResponse.getCandles() == null || responseString == null)
        throw new StockQuoteServiceException("Invalid Response Found");
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException(e.getMessage());
    }
    List<Candle> alphavantageCandles = new ArrayList<>();
    Map<LocalDate, AlphavantageCandle> mapOFDateAndAlphavantageCandle =
        alphavantageDailyResponse.getCandles();
    for (LocalDate localDate : mapOFDateAndAlphavantageCandle.keySet()) {
      if (localDate.isAfter(from.minusDays(1)) && localDate.isBefore(to.plusDays(1))) {
        AlphavantageCandle alphavantageCandle =
            alphavantageDailyResponse.getCandles().get(localDate);
        alphavantageCandle.setDate(localDate);
        alphavantageCandles.add(alphavantageCandle);
      }
    }
    return alphavantageCandles.stream().sorted(Comparator.comparing(Candle::getDate))
        .collect(Collectors.toList());
  }


  // https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=IBM&outputsize=full&apikey=demo
  // Method to Build URL
  protected String buildURL(String symbol) {
    String uriTemplate = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + symbol + "&outputsize=full&apikey=" + getToken();
    return uriTemplate;
  }

  protected String getToken() {
    // Replace "YOUR_API_KEY_HERE" with your actual Alpha Vantage API key
    return "O5BAY5ZAY65G6UB6";
}


  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  

}
