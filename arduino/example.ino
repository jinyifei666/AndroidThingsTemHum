#include <Wire.h>


// подключаем библиотеку
#include "dht.h"

// создаём объект-сенсор
DHT sensor = DHT();
int t=0;
int h=0;

void setup()
{
    Serial.begin(9600);

    // методом attach объявляем к какому контакту подключен
    // сенсор. В нашем примере это нулевой аналоговый контакт
    sensor.attach(A0);
    //
    // после подачи питания ждём секунду до готовности сенсора к работе
    delay(1000);
   
    Wire.begin(8); 
    Wire.onRequest(requestEvent);
}

void loop()
{
    // метод update заставляет сенсор выдать текущие измерения
    sensor.update();

    switch (sensor.getLastError())
    {
        case DHT_ERROR_OK:
            char msg[128];
            t=sensor.getTemperatureInt();
            h=sensor.getHumidityInt();
            // данные последнего измерения можно считать соответствующими
            // методами
            sprintf(msg, "Temperature = %dC, Humidity = %d%%", 
                    sensor.getTemperatureInt(), sensor.getHumidityInt());
            Serial.println(msg);
            break;
        case DHT_ERROR_START_FAILED_1:
            Serial.println("Error: start failed (stage 1)");
            break;
        case DHT_ERROR_START_FAILED_2:
            Serial.println("Error: start failed (stage 2)");
            break;
        case DHT_ERROR_READ_TIMEOUT:
            Serial.println("Error: read timeout");
            break;
        case DHT_ERROR_CHECKSUM_FAILURE:
            Serial.println("Error: checksum error");
            break;
    }
    delay(2000);
}

void requestEvent() {
  char data[7];
  sprintf(data,"T%dH%dE",t,h);
  Wire.write(data); // respond with message of 5 bytes
  // as expected by master
}
