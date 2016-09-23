/* ---------------------------- */
// ROVER 5 CODE                 //
// AUTHOR: DANIEL MANCEBO       //
// DATE: 01/10/2013             //
/* ---------------------------- */

#include <SoftwareSerial.h>   

#define RxD 2                
#define TxD 3

#define DIR_L    4      // Polaridad del motor izquierdo
#define MOTOR_L  5      // PWM Pin del motor izquierdo
#define MOTOR_R  6      // PWM Pin del motor derecho
#define DIR_R    7      // Polaridad del motor derecho

SoftwareSerial bluetoothSerial(RxD, TxD);
  
/**  
 * Funcion que detiene los motores
 */
void halt(void) 
{                                
   analogWrite(MOTOR_R, 0);
   analogWrite(MOTOR_L, 0);
   
   delay(100);
}

/**
 * Funcion que mueve los motores dependiendo del parametro de entrada
 */
void motors (short int y) 
{          
  if (y == 0) 
  {                               // 0 = retroceder
     digitalWrite(DIR_R, HIGH);
     digitalWrite(DIR_L, HIGH);
     analogWrite(MOTOR_R, 80);
     analogWrite(MOTOR_L, 80);
     
  } else if (y == 1) 
  {                               // 1 = girar derecha
     digitalWrite(DIR_R, LOW);
     digitalWrite(DIR_L, LOW);
     analogWrite(MOTOR_R, 0);
     analogWrite(MOTOR_L, 100);
     
  } else if (y == 2) 
  {                               // 2 = girar izquierda
     digitalWrite(DIR_R, LOW);
     digitalWrite(DIR_L, LOW);
     analogWrite(MOTOR_R, 100);
     analogWrite(MOTOR_L, 0);
     
  } else if (y == 3) 
  {                               // 3 = avanzar
     digitalWrite(DIR_R, LOW );
     digitalWrite(DIR_L, LOW );
     analogWrite(MOTOR_R, 150 );
     analogWrite(MOTOR_L, 150 ); 
  }
}

void setupBlueToothConnection()
{
    bluetoothSerial.begin(38400); 
    
    delay(1000);
    
    sendBlueToothCommand("\r\n+STWMOD=0\r\n");                // 0 = slave
    sendBlueToothCommand("\r\n+STNA=SedBlutoth-st_XX\r\n" ); 
    sendBlueToothCommand("\r\n+STAUTO=0\r\n");                // 0 = Auto-connect forbidden
    sendBlueToothCommand("\r\n+STOAUT=1\r\n");                // 1 = Permit Paired device to connect me
    sendBlueToothCommand("\r\n+STPIN=0000\r\n");
    
    delay(2000); // This delay is required.
    
    sendBlueToothCommand("\r\n+INQ=1\r\n");        // 1 = Enable been inquired (slave)
    
    delay(2000); // This delay is required.
    
    bluetoothSerial.print("You are connected!");
}

void checkOK() 
{
  char a, b;
  
  while (1) 
  {
    if (bluetoothSerial.available())
    {
      a = bluetoothSerial.read();
// ************************************* debug ********************************************
      Serial.write(a);    // show response from bluetooth module

      if ('O' == a)
      {
        // Wait for next character K. available() is required in some cases, as K is not immediately available.
        while (bluetoothSerial.available())
        {
           b = bluetoothSerial.read();
          
  // ************************************* debug ********************************************
           Serial.write(b);         
           break;
        }
        
        if ('K' == b)
        {
          break;
        }
      }
    }
  }

  while((a = bluetoothSerial.read()) != -1)
  {
    // Wait until all other response chars are received
  }
}

void sendBlueToothCommand(char command[])
{
    bluetoothSerial.print(command);
    checkOK( );   
}

void setup() 
{
   pinMode(DIR_R, OUTPUT);
   pinMode(DIR_L, OUTPUT);
   pinMode(MOTOR_R, OUTPUT);
   pinMode(MOTOR_L, OUTPUT);
   
   pinMode(RxD, INPUT);
   pinMode(TxD, OUTPUT);
   
   Serial.begin(9600);
   setupBlueToothConnection();  
}

void loop() 
{
    if (bluetoothSerial.available( ))
    {
        char command = (char)bluetoothSerial.read();
        bluetoothSerial.flush();
            
        switch (command) 
        {
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
    }
} // loop
