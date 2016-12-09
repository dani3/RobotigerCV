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
#define RIGHT         1     // Turn right command
#define LEFT          2     // Turn left command
#define FORWARD       3     // Move forward command

#define START     0
#define STOP      1

#define UNUSED   -1

#define NUMBER_OF_SERVOS    5
#define PIN_OFFSET          8

#define GET_SERVO_PIN(_pos) (_pos + PIN_OFFSET)

const int MIN_DEGREES_WRIST = 45;
const int MAX_DEGREES_WRIST = 120;

// Type of instruction received.
byte _instruction;
// Parameter received if needed.
byte _arg;

// Array of servos to control the arm.
VarSpeedServo _servos[NUMBER_OF_SERVOS];

/**
 * Function to rotate the wrist to the position received.
 * @param pos: value between 0 and 100.
 */
void _rotateWrist(short int pos)
{
 
}

/**
 * Function to stop the robot.
 */
void _halt(void) 
{                                
  analogWrite(MOTOR_R, 0);
  analogWrite(MOTOR_L, 0);
   
  delay(100);
}

/**
 * Funtion to move the robot.
 * @param command: command received from app.
 */
void _move(short int command) 
{          
  if (command == BACK) 
  {                               
    digitalWrite(DIR_R, HIGH);
    digitalWrite(DIR_L, HIGH);
    analogWrite(MOTOR_R, 80);
    analogWrite(MOTOR_L, 80);
     
  } 
  else if (command == RIGHT) 
  {                               
    digitalWrite(DIR_R, HIGH);
    digitalWrite(DIR_L, LOW);
    analogWrite(MOTOR_R, 100);
    analogWrite(MOTOR_L, 100);
     
  } 
  else if (command == LEFT) 
  {                              
    digitalWrite(DIR_R, LOW);
    digitalWrite(DIR_L, HIGH);
    analogWrite(MOTOR_R, 100);
    analogWrite(MOTOR_L, 100);
     
  } 
  else if (command == FORWARD) 
  {                              
    digitalWrite(DIR_R, LOW );
    digitalWrite(DIR_L, LOW );
    analogWrite(MOTOR_R, 150 );
    analogWrite(MOTOR_L, 150 ); 
  }
}

/**
 * Bluetooth SETUP
 */
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

/**
 * Setup
 */
void setup() 
{
  // Attach every servo.
  for (int i = 0; i < NUMBER_OF_SERVOS; i++)
  {
    _servos[i].attach(GET_SERVO_PIN(i));
  }

  // TODO Set the initial position of the servos.

  pinMode(DIR_R, OUTPUT);
  pinMode(DIR_L, OUTPUT);
  pinMode(MOTOR_R, OUTPUT);
  pinMode(MOTOR_L, OUTPUT);
  
  digitalWrite(MOTOR_R, 0);
  digitalWrite(MOTOR_L, 0);
   
  setupBlueToothConnection(); 
  Serial.flush();  
}

/**
 * Main LOOP
 */
void loop() 
{    
  if (Serial.available())
  {
    _instruction = Serial.read();
    _arg = Serial.read();
    Serial.flush();
        
    switch (_instruction) 
    {
      case 0x00:              // Move back
        _move(BACK);         
        break;    
            
      case 0x10:              // Turn right
        _move(RIGHT);         
        break;   
             
      case 0x20:              // Turn left
        _move(LEFT);          
        break;  
              
      case 0x30:              // Move forward
        _move(FORWARD);       
        break; 
              
      case 0x40:              // Stop
        _halt();         
        break;  

      case 0x50:              // Rotate wrist
        _rotateWrist((short int) _arg);         
        break;
    }
  }
}
