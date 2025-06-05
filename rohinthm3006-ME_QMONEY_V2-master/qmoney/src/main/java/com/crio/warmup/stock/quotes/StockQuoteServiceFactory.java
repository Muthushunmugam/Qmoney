package com.crio.warmup.stock.quotes;

import org.springframework.web.client.RestTemplate;

public enum StockQuoteServiceFactory {

  INSTANCE;

  public StockQuotesService getService(String provider, RestTemplate restTemplate) {
    if (provider != null) {
      switch (provider.toLowerCase()) {
        case "tiingo":
          return new TiingoService(restTemplate);
        case "alphavantage":
          return new AlphavantageService(restTemplate);  // Add this case
        // Optionally handle other providers if needed
        // case "anotherProvider":
        //     return new AnotherProviderService(restTemplate);
        default:
         // throw new IllegalArgumentException("Unsupported provider: " + provider);
         return new AlphavantageService(restTemplate); 
      }
    } else {
      // If no provider is provided, you could throw an exception or return null
    
    }
   return null ;
  }
}
