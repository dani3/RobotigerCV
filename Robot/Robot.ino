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

SoftwareSerial bluetoothSerial( RxD,TxD );
  
// Funcion que detiene los motores  
void halt ( ) 
{                                
   analogWrite( MOTOR_R, 0 );
   analogWrite( MOTOR_L, 0 );
   delay( 100 );
}


// Funcion que mueve los motores dependiendo del parametro de entrada
void motors ( short int y ) 
{          
  if ( y == 0 ) 
  {                               // 0 = retroceder
     digitalWrite( DIR_R, HIGH );
     digitalWrite( DIR_L, HIGH );
     analogWrite( MOTOR_R, 150 );
     analogWrite( MOTOR_L, 150 );
     
  } else if (y == 1) 
  {                          // 1 = girar derecha
     digitalWrite( DIR_R, LOW );
     digitalWrite( DIR_L, LOW );
     analogWrite( MOTOR_R, 0 );
     analogWrite( MOTOR_L, 100 );
     
  } else if ( y == 2 ) 
  {                          // 2 = girar izquierda
     digitalWrite( DIR_R, LOW );
     digitalWrite( DIR_L, LOW );
     analogWrite( MOTOR_R, 100 );
     analogWrite( MOTOR_L, 0 );
     
  } else if ( y == 3 ) 
  {                          // 3 = avanzar
     digitalWrite( DIR_R, LOW );
     digitalWrite( DIR_L, LOW );
     analogWrite( MOTOR_R, 150 );
     analogWrite( MOTOR_L, 150 ); 
  }
}


void setup( ) 
{
   pinMode( DIR_R, OUTPUT );
   pinMode( DIR_L, OUTPUT );
   pinMode( MOTOR_R, OUTPUT );
   pinMode( MOTOR_L, OUTPUT );
   
   pinMode( RxD, INPUT );
   pinMode( TxD, OUTPUT );
  
   bluetoothSerial.begin( 9600 );
   bluetoothSerial.flush(  );
   delay( 500 );
 
   Serial.begin( 9600 );
   Serial.println( "Ready" );  
}


void loop( ) 
{
    if ( bluetoothSerial.available( ) )
    {
        char command = ( char )bluetoothSerial.read( );
        bluetoothSerial.flush( );
            
        switch ( command ) {
          case 'x':
              motors( 0 );         
          break;      
          case 'd':
              motors( 1 );         
          break;      
          case 'a':
              motors( 2 );          
          break;      
          case 'w':
              motors( 3 );       
          break;      
          case 's':
              halt( );         
          break;  
        } // switch
    }
} // loop
