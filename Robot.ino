/* ---------------------------- */
// ROVER 5 CODE                 //
// AUTHOR: DANIEL MANCEBO       //
// DATE: 01/10/2013             //
/* ---------------------------- */


#include <SoftwareSerial.h>    //Software Serial Port

#define RxD 2                  // these '2' & '3' are fixed by the hardware
#define TxD 3
#define DIR_R 7                // Polaridad del motor derecho
#define MOTOR_R 6              // PWM Pin del motor derecho
#define DIR_L 4                // Polaridad del motor izquierdo
#define MOTOR_L 5              // PWM Pin del motor izquierdo
#define ECHO 8                 // Pin para recoger el pulso
#define TRIGGER 9              // Pin que dispara un pulso

#define DEBUG_ENABLED  1

SoftwareSerial blueToothSerial(RxD,TxD);

unsigned int tiempo, distancia;

// 00 13 EF 00 0E 5F

// Funcion que devuelve la distanca medida en CM
unsigned int rangeInCM (short int sensor) {         
   digitalWrite(TRIGGER, LOW);
   delayMicroseconds(2);
   digitalWrite(TRIGGER, HIGH);
   delayMicroseconds(10);
   digitalWrite(TRIGGER, LOW);
   
   tiempo = pulseIn(ECHO, HIGH);              // Calcula la distancia midiendo el tiempo del estado alto del pin ECHO   
   distancia= tiempo/58;                      // La velocidad del sonido es de 340m/s o 29 microsegundos por centimetro
   
   return distancia;
} // rangeInCM
  
  
// Funcion que detiene los motores  
void halt () {                                
   analogWrite(MOTOR_R, 0);
   analogWrite(MOTOR_L, 0);
   delay(100);
}


// Funcion que mueve los motores dependiendo del parametro de entrada
void motors (short int y) {          
  if (y == 0) {                               // 0 = retroceder
     digitalWrite(DIR_R, HIGH);
     digitalWrite(DIR_L, HIGH);
     analogWrite(MOTOR_R, 150);
     analogWrite(MOTOR_L, 150);
  }
  else if (y == 1) {                          // 1 = girar derecha
     digitalWrite(DIR_R, LOW);
     digitalWrite(DIR_L, LOW);
     analogWrite(MOTOR_R, 0);
     analogWrite(MOTOR_L, 100);
  }
  else if (y == 2) {                          // 2 = girar izquierda
     digitalWrite(DIR_R, LOW);
     digitalWrite(DIR_L, LOW);
     analogWrite(MOTOR_R, 100);
     analogWrite(MOTOR_L, 0);
  }
  else if (y == 3) {                          // 3 = avanzar
     digitalWrite(DIR_R, LOW);
     digitalWrite(DIR_L, LOW);
     analogWrite(MOTOR_R, 150);
     analogWrite(MOTOR_L, 150); 
  }
}


void setup() {
   Serial.begin(9600);
   pinMode(DIR_R, OUTPUT);
   pinMode(DIR_L, OUTPUT);
   pinMode(MOTOR_R, OUTPUT);
   pinMode(MOTOR_L, OUTPUT);
   pinMode(RxD, INPUT);
   pinMode(TxD, OUTPUT);
   pinMode(TRIGGER, OUTPUT);
   pinMode(ECHO, INPUT); 
   setupBlueToothConnection();
}


void loop() {
  unsigned char temp;
  char val;                            // Variable donde almacenamos la tecla pulsada
 
  while(temp!='4')  {
      temp=blueToothSerial.read();     
  }
 
  Serial.println("You are connected!");
  blueToothSerial.print("You are connected!");      

  // You can write you BT communication logic here 
 
  while(1) {  
    val = ' ';
    
    if (blueToothSerial.available())
        val = (char)blueToothSerial.read();
        
    switch (val) {
      case 'x':
          motors(0);         
      break;      
      case 'd':
          motors(1);         
      break;      
      case 'a':
          motors(2);          
      break;      
      case 'w':
          motors(3);       
      break;      
      case 's':
          halt();         
      break;  
    } // switch
  } // while 
} // loop



void setupBlueToothConnection() {
    blueToothSerial.begin(38400);                       //Set BluetoothBee BaudRate to default baud rate 38400
    delay(1000);
    sendBlueToothCommand("\r\n+STWMOD=0\r\n");          // 0 = slave
    sendBlueToothCommand("\r\n+STNA=SedBlutoth-st_XX\r\n"); 
    sendBlueToothCommand("\r\n+STAUTO=0\r\n");          // 0 = Auto-connect forbidden
    sendBlueToothCommand("\r\n+STOAUT=1\r\n");          // 1 = Permit Paired device to connect me
    sendBlueToothCommand("\r\n+STPIN=0000\r\n");
    delay(2000); // This delay is required.
    sendBlueToothCommand("\r\n+INQ=1\r\n");             // 1 = Enable been inquired (slave)
    delay(2000); // This delay is required.
} // setupBlueToothConnection

//Checks if the response "OK" is received
void CheckOK() {
  char a,b;
  while(1) {
    if(blueToothSerial.available())
    {
    a = blueToothSerial.read();
// ************************************* debug ********************************************
    Serial.write(a);    // show response from bluetooth module

    if('O' == a)
    {
      // Wait for next character K. available() is required in some cases, as K is not immediately available.
      while(blueToothSerial.available())
      {
         b = blueToothSerial.read();
        
// ************************************* debug ********************************************
    Serial.write(b);        
        
         break;
      }
      if('K' == b)
      {
        break;
      }
     }
    }
   }

  while( (a = blueToothSerial.read()) != -1)
  {
    //Wait until all other response chars are received
  }
} // CheckOK

void sendBlueToothCommand(char command[]) {
    blueToothSerial.print(command);
    CheckOK();   
} // sendBlueToothCommand
