package org.crpt;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final int requestLimit;
    private final Semaphore requestSemaphore;
    private final ScheduledExecutorService scheduler; //интерфейс выполнения задач по расписанию.


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;

        this.requestSemaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1); //создаём планировщик с 1 потоком.
        scheduler.scheduleAtFixedRate(this::resetSemaphore, 0, 1, timeUnit); //раз в 1 ед. времени вызываем освобождение всех разрешений семафора.
    }

    /** освобождаем разрешения семафора */
    private void resetSemaphore() {
        int usedPermits = requestLimit - requestSemaphore.availablePermits();
        requestSemaphore.release(usedPermits);
    }

    public void createDocument(String document, String signature) {
        try {
            System.out.println("Документ: " + document + " - запрос разрешения");
            requestSemaphore.acquire(); // запрашиваем разрешение
            sendApiRequest(document, signature);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendApiRequest(String document, String signature) {
        //TODO: добавить API-запрос используя http-клинет
        //Для демонстрации
        try {
            String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            HttpClient httpClient = HttpClients.createDefault();
            System.out.println("Документ: " + document + " - запрос");
            HttpPost httpPost = new HttpPost(apiUrl);

            //установка хедеров и тела запроса
            String reqBody = "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", 109 \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(reqBody, ContentType.APPLICATION_JSON));

            HttpResponse response = httpClient.execute(httpPost);

            //получаем статус ответа, если нужен
            int statusCode = response.getStatusLine().getStatusCode();
            // ...

        } catch (IOException e) {
            e.printStackTrace(); //Обработка исключения
        }
    }

    /** Завершение работы планировщика */
    public void shutdown() {
        scheduler.shutdown();
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);

        // Example usage
        for (int i = 0; i < 15; i++) {
            crptApi.createDocument("sampleDocument #" + i, "sampleSignature #");
        }

        crptApi.shutdown();
    }


}
