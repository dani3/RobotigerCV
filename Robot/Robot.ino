#include <VarSpeedServo.h>

/* ---------------------------- */
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

#define UNUSED   -1

#define NUMBER_OF_SERVOS    5
#define PIN_OFFSET          8

#define SERVO_WRIST_POSITION   0

#define SERVO_SPEED     32

#define GET_SERVO_PIN(_pos) (_pos + PIN_OFFSET)

const short OLD_MIN_RANGE = 0;
const short OLD_MAX_RANGE = 100;

const int MIN_DEGREES_WRIST = 50;
const int MAX_DEGREES_WRIST = 111;

// Type of instruction received.
short _instruction;
// Parameter received if needed.
short _arg;

// Array of servos to control the arm.
VarSpeedServo _servos[NUMBER_OF_SERVOS];

/**
 * Function to normalize the value received to the working range of the servo.
 * @param oldValue: value received between 0-100 to be normalized.
 * @param minNewRange: lowest value of the new range.
 * @param maxNewRange: highest value of the new range.
 */
int _normalize(short oldValue, int minNewRange, int maxNewRange)
{
  return (float)(maxNewRange - minNewRange) / (float)(OLD_MAX_RANGE - OLD_MIN_RANGE) * (float)(oldValue - OLD_MAX_RANGE) + (float)maxNewRange;
}

/**
 * Function to rotate the wrist to the position received.
 * @param pos: value between 0 and 100.
 */
void _rotateWrist(short pos)
{
  int newPosition = _normalize(pos, MIN_DEGREES_WRIST, MAX_DEGREES_WRIST);
  
  _servos[SERVO_WRIST_POSITION].slowmove(newPosition, SERVO_SPEED);
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
void _move(short command) 
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
    analogWrite(MOTOR_R, 150);
    analogWrite(MOTOR_L, 150); 
  }
}

/**
 * Bluetooth SETUP
 */
void setupBlueToothConnection()
{
  // Set BluetoothBee BaudRate to default baud rate 38400.
  Serial.begin(38400);     
                     
  // Set the bluetooth work in slave mode.
  Serial.print("\r\n+STWMOD=0\r\n");     
  // Set the bluetooth name as "SeeedBTSlave". 
  Serial.print("\r\n+STNA=SeeedBTSlave\r\n"); 
  // Permit Paired device to connect me.
  Serial.print("\r\n+STOAUT=1\r\n");    
  // Auto-connection should be forbidden here.    
  Serial.print("\r\n+STAUTO=0\r\n");          

  // This delay is required.
  delay(2000); 
  
  // Make the slave bluetooth inquirable.
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

  // Init servos position.
  _servos[SERVO_WRIST_POSITION].slowmove(MIN_DEGREES_WRIST, SERVO_SPEED);

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
  // Wait until instruction byte has been received.
  while (!Serial.available());
  // Instruction should be read. MSB is set to 1.
  _instruction = (short) Serial.read();
  
  if (_instruction & 0x80)
  {
    // Wait until arg byte has been received.
    while (!Serial.available());
    // Read the argument.
    _arg = (short) Serial.read();

    switch (_instruction) 
    {
      case 0x80:              // Move back
        _move(BACK);         
        break;    
            
      case 0x81:              // Turn right
        _move(RIGHT);         
        break;   
             
      case 0x82:              // Turn left
        _move(LEFT);          
        break;  
              
      case 0x83:              // Move forward
        _move(FORWARD);       
        break; 
              
      case 0x84:              // Stop
        _halt();         
        break;  
  
      case 0xA0:              // Rotate wrist
        _rotateWrist(_arg);         
        break;
    }
  }
  
  Serial.flush();
}
