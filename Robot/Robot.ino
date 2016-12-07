#include <VarSpeedServo.h>

/* ---------------------------- */
/* ROVER 5 CODE                 */
/* AUTHOR: DANIEL MANCEBO       */
/* DATE: 01/10/2013             */
/* ---------------------------- */

#define DIR_L      4      // Left motor polarity
#define MOTOR_L    5      // Left motor PIN
#define MOTOR_R    6      // Right motor PIN
#define DIR_R      7      // Right motor polarity

#define BACK          0     // Move back command 
#define TURN_RIGHT    1     // Turn right command
#define TURN_LEFT     2     // TUrn left command
#define FORWARD       3     // Move forward command

#define NUMBER_OF_SERVOS    5
#define PIN_OFFSET          8

#define GET_SERVO_PIN(_pos) (_pos + PIN_OFFSET)

// Command received from Android app
char _command;

// Array of servos to control the robotic arm
VarSpeedServo _servos[NUMBER_OF_SERVOS];


void _halt(void) 
{                                
  analogWrite(MOTOR_R, 0);
  analogWrite(MOTOR_L, 0);
   
  delay(100);
}


void _move(short int y) 
{          
  if (y == BACK) 
  {                               
    digitalWrite(DIR_R, HIGH);
    digitalWrite(DIR_L, HIGH);
    analogWrite(MOTOR_R, 80);
    analogWrite(MOTOR_L, 80);
     
  } else if (y == TURN_RIGHT) 
  {                               
    digitalWrite(DIR_R, HIGH);
    digitalWrite(DIR_L, LOW);
    analogWrite(MOTOR_R, 100);
    analogWrite(MOTOR_L, 100);
     
  } else if (y == TURN_LEFT) 
  {                              
    digitalWrite(DIR_R, LOW);
    digitalWrite(DIR_L, HIGH);
    analogWrite(MOTOR_R, 100);
    analogWrite(MOTOR_L, 100);
     
  } else if (y == FORWARD) 
  {                              
    digitalWrite(DIR_R, LOW );
    digitalWrite(DIR_L, LOW );
    analogWrite(MOTOR_R, 150 );
    analogWrite(MOTOR_L, 150 ); 
  }
}


void setupBlueToothConnection()
{
  // Set BluetoothBee BaudRate to default baud rate 38400
  Serial.begin(38400);     
                     
  // Set the bluetooth work in slave mode
  Serial.print("\r\n+STWMOD=0\r\n");     
  // Set the bluetooth name as "SeeedBTSlave"     
  Serial.print("\r\n+STNA=SeeedBTSlave\r\n"); 
  // Permit Paired device to connect me
  Serial.print("\r\n+STOAUT=1\r\n");    
  // Auto-connection should be forbidden here      
  Serial.print("\r\n+STAUTO=0\r\n");          

  // This delay is required.
  delay(2000); 
  
  // Make the slave bluetooth inquirable 
  Serial.print("\r\n+INQ=1\r\n"); 

  // This delay is required.
  delay(2000); 
  
  Serial.flush();  
}


void setup() 
{
  for (int i = 0; i < NUMBER_OF_SERVOS; i++)
  {
    _servos[i].attach(GET_SERVO_PIN(i));
  }

  pinMode(DIR_R, OUTPUT);
  pinMode(DIR_L, OUTPUT);
  pinMode(MOTOR_R, OUTPUT);
  pinMode(MOTOR_L, OUTPUT);
  
  digitalWrite(MOTOR_R, 0);
  digitalWrite(MOTOR_L, 0);
   
  setupBlueToothConnection(); 
  Serial.flush();  
}


void loop() 
{    
  if (Serial.available())
  {
    _command = (char)Serial.read();
    Serial.flush();
        
    switch (_command) 
    {
      case 'x':              // Move back
        _move(BACK);         
        break;    
            
      case 'd':              // Turn right
        _move(TURN_RIGHT);         
        break;   
             
      case 'a':              // Turn left
        _move(TURN_LEFT);          
        break;  
              
      case 'w':              // Move forward
        _move(FORWARD);       
        break; 
              
      case 's':              // Stop
        _halt();         
        break;  
    }
  }
}
