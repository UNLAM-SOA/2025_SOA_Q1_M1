/****************************************************** 
                      Libraries 
******************************************************/
#include <WiFi.h>
#include <PubSubClient.h>

/****************************************************** 
                        Macros 
******************************************************/
// Pin map
#define PIN_LED_GREEN                       2
#define PIN_PRESSURE_SENSOR                 4
#define PIN_LED_RED                         5
#define PIN_TRIG_RIGHT_DISTANCE_SENSOR      13
#define PIN_BUTTON_ON                       15
#define PIN_TRIG_LEFT_DISTANCE_SENSOR       19
#define PIN_ECHO_LEFT_DISTANCE_SENSOR       21
#define PIN_VIBRATION_MOTOR                 22
#define PIN_ECHO_RIGHT_DISTANCE_SENSOR      23
#define PIN_SOUND_BUTTON                    32
#define PIN_BUZZER                          18

// Sensor configurations
#define TIME_ULTRASONIC_2                   2
#define TIME_ULTRASONIC_10                  10
#define DISTANCE_MEDIUM                     50
#define DISTANCE_CLOSE                      30
#define DISTANCE_VERY_CLOSE                 15
#define DISTANCE_THRESHOLD                  50 
#define PRESSUSURE_THRESHOLD                2000
#define DIFFERENCE_THRESHOLD_TIMEOUT        50
#define MAX_NUM_ANALOG_SEN                  1
#define MAX_BUTTON_COUNT                    2
#define MAX_NUM_SENSOR                      3
#define PRESSURE_SENSOR                     0
#define LEFT_DISTANCE_SENSOR                0
#define RIGHT_DISTANCE_SENSOR               1
#define POWER_ON_BUTTON                     0
#define ALARM_SOUND_BUTTON                  1

// Actuator configurations
#define TIMEOUT_CANE_RELEASED               3000
#define ITERATIONS_ALARM_CANE_RELEASED      1
#define ITERATIONS_OBSTACLE_ALARM_LEFT      1
#define ITERATIONS_OBSTACLE_ALARM_RIGHT     2
#define ITERATIONS_OBSTACLE_ALARM_BOTH      3
#define SOUND_SPEED                         0.034

// Buzzer tones
#define TONE_BUZZER_LOW                     400
#define TONE_BUZZER_MEDIUM                  1000
#define TONE_BUZZER_HIGH                    1800
#define TONE_ALARM_CANE_RELEASED            200

// State machine configurations
#define MAX_STATES                          9
#define MAX_EVENTS                          17

// General macros
#define INITIAL_SENSOR_VALUE                -1
#define NO_WAIT                             0
#define ON                                  1
#define OFF                                 0
#define CONNECTED                           1
#define DISCONNECTED                        0

// Task configurations
#define BUZZER_TASK_STACK_SIZE              2048
#define MOTOR_TASK_STACK_SIZE               2048
#define MQTT_TASK_STACK_SIZE                2048
#define BUZZER_TASK_PRIORITY                1
#define MOTOR_TASK_PRIORITY                 1
#define MQTT_TASK_PRIORITY                  1
#define QUEUE_SIZE                          1
#define DELAY_TASK                          100
#define LONG_DELAY_TASK                     300
#define SMALL_DELAY                         10 

// WiFi y MQTT
#define TOPIC_MQTT_CMD_EMQX                 "soa1c2025/smartcane/cmd"
#define TOPIC_MQTT_ON_OFF_EMQX              "soa1c2025/smartcane/on_off_status"
#define TOPIC_MQTT_OBSTACLE_STATUS_EMQX     "soa1c2025/smartcane/obstacle_status"
#define TOPIC_MQTT_ALARM_STATUS_EMQX        "soa1c2025/smartcane/alarm_status"
#define INITIAL_MQTT_COMMAND                -1
#define TOPIC_NAME_LENGTH                   1000
#define CREDENTIALS_LENGTH                  100
#define MQTT_PORT                           1883
#define WIFI_CONNECTION_DELAY               500
#define WIFI_RECONNECTION_DELAY             5000

// Debug
#define SERIAL_DEBUG_ENABLED 1

#if SERIAL_DEBUG_ENABLED
  #define DebugPrint(str)\
    {\
      Serial.print(str);\
    }
#else
  #define DebugPrint(str)
#endif

#if SERIAL_DEBUG_ENABLED
  #define DebugPrintln(str)\
    {\
      Serial.println(str);\
    }
#else
  #define DebugPrint(str)
#endif

#if SERIAL_DEBUG_ENABLED
  #define DebugPrintState(state,event)\
    {\
      String st = state;\
      String ev = event;\
      String str;\
      str = "-----------------------------------------------------";\
      DebugPrintln(str);\
      str = "STATE-> [" + st + "]: " + "EVENT-> [" + ev + "].";\
      DebugPrintln(str);\
      str = "-----------------------------------------------------";\
      DebugPrintln(str);\
    }
#else
  #define DebugPrintEstado(estado,evento)
#endif


/****************************************************** 
                    General Structs 
******************************************************/
struct st_button
{
  int  pin;
  long current_value;
  long previous_value;
};
st_button buttons[MAX_BUTTON_COUNT];

struct stSensorAnalog
{
  int  pin;
  long current_value;
  long previous_value;
};
stSensorAnalog analog_sensors[MAX_NUM_ANALOG_SEN];

struct stSensorDist
{
  int  pin_echo;
  int  pin_trig;
  long current_value;
  long previous_value;
};
stSensorDist sensores_distancia[MAX_NUM_SENSOR-MAX_NUM_ANALOG_SEN];

/****************************************************** 
            Transition Function Declaration 
******************************************************/
// Switch functions
void turn_on();
void turn_off();

// Alarm sound functions
void sound_alarm_obstacle_on();
void sound_alarm_obstacle_off();

// Stick press/release
void alarm_stick_off();
void alarm_stick_on();
void start_timer_cane_released();

// Obstacle alarms
void alarm_collision_right_on();
void alarm_collision_left_on();
void alarm_collision_both_on();
void alarm_collision_off();

void mqttConnect();
void wifiConnect();
void shut_down_system();
void set_active_state();
void set_wifi_disconnected_state();
void set_mqtt_disconnected_state();

// Default handlers
void error();
void none();

/****************************************************** 
                      State Machine 
******************************************************/
enum states          { ST_OFF,
                       ST_ACTIVE,
                       ST_MQTT_DISCONNECTED,
                       ST_WIFI_DISCONNECTED,
                       ST_TIMER_WAITING,
                       ST_CANE_REL,
                       ST_RIGHT_OBSTACLE,
                       ST_LEFT_OBSTACLE,
                       ST_BOTH_OBSTACLE} current_state;

String states_s [] = { "ST_OFF",
                       "ST_ACTIVE",
                       "ST_MQTT_DISCONNECTED",
                       "ST_WIFI_DISCONNECTED",
                       "ST_TIMER_WAITING",
                       "ST_CANE_REL",
                       "ST_RIGHT_OBSTACLE",
                       "ST_LEFT_OBSTACLE",
                       "ST_BOTH_OBSTACLE"};

enum events          {EV_SWITCH_ON, 
                      EV_SWITCH_OFF,
                      EV_MQTT_CONNECTED,
                      EV_MQTT_DISCONNECTED,
                      EV_WIFI_CONNECTED,
                      EV_WIFI_DISCONNECTED,
                      EV_SWITCH_SOUND_ON,
                      EV_SWITCH_SOUND_OFF,
                      EV_CONT, 
                      EV_PRESSED,
                      EV_TIMEOUT_RELEASED,
                      EV_NOT_PRESSED, 
                      EV_OBSTACLE_RIGHT,
                      EV_NO_OBSTACLE_RIGHT, 
                      EV_OBSTACLE_LEFT,
                      EV_NO_OBSTACLE_LEFT,
                      EV_TIMEOUT } new_event;

String events_s [] = {"EV_SWITCH_ON",
                      "EV_SWITCH_OFF",
                      "EV_MQTT_CONNECTED",
                      "EV_MQTT_DISCONNECTED",
                      "EV_WIFI_CONNECTED",
                      "EV_WIFI_DISCONNECTED",
                      "EV_SWITCH_SOUND_ON",
                      "EV_SWITCH_SOUND_OFF",
                      "EV_CONT",
                      "EV_PRESSED",
                      "EV_TIMEOUT_RELEASED",
                      "EV_NOT_PRESSED",
                      "EV_OBSTACLE_RIGHT",
                      "EV_NO_OBSTACLE_RIGHT", 
                      "EV_OBSTACLE_LEFT",
                      "EV_NO_OBSTACLE_LEFT",
                      "EV_TIMEOUT"};

typedef void (*transition)();
transition state_table[MAX_EVENTS][MAX_STATES] =
{
  // ST_OFF    ST_ACTIVE                    ST_MQTT_DISCONNECTED            ST_WIFI_DISCONNECTED   ST_TIMER_WAITING    ST_CANE_REL                    ST_RIGHT_OBSTACLE              ST_LEFT_OBSTACLE               ST_BOTH_OBSTACLE
  { turn_on  , none                         , none                          , none                 , none              , none                         , none                         , none                         , none                         }, // EV_SWITCH_ON
  { none     , turn_off                     , none                          , none                 , turn_off          , turn_off                     , turn_off                     , turn_off                     , turn_off                     }, // EV_SWITCH_OFF
  { none     , none                         , set_active_state              , none                 , none              , none                         , none                         , none                         , none                         }, // EV_MQTT_CONNECTED
  { none     , set_mqtt_disconnected_state  , mqttConnect                   , none                 , none              , set_mqtt_disconnected_state  , set_mqtt_disconnected_state  , set_mqtt_disconnected_state  , set_mqtt_disconnected_state  }, // EV_MQTT_DISCONNECTED
  { none     , none                         , none                          , set_active_state     , none              , none                         , none                         , none                         , none                         }, // EV_WIFI_CONNECTED
  { none     , set_wifi_disconnected_state  , set_wifi_disconnected_state   , wifiConnect          , none              , set_wifi_disconnected_state  , set_wifi_disconnected_state  , set_wifi_disconnected_state  , set_wifi_disconnected_state  }, // EV_WIFI_DISCONNECTED
  { none     , none                         , none                          , none                 , none              , none                         , sound_alarm_obstacle_on      , sound_alarm_obstacle_on      , sound_alarm_obstacle_on      }, // EV_SWITCH_SOUND_ON
  { none     , none                         , none                          , none                 , none              , none                         , sound_alarm_obstacle_off     , sound_alarm_obstacle_off     , sound_alarm_obstacle_off     }, // EV_SWITCH_SOUND_OFF
  { none     , none                         , none                          , none                 , none              , none                         , none                         , none                         , none                         }, // EV_CONT
  { none     , none                         , none                          , none                 , alarm_stick_off   , alarm_stick_off              , none                         , none                         , none                         }, // EV_PRESSED
  { none     , none                         , none                          , none                 , alarm_stick_on    , none                         , none                         , none                         , none                         }, // EV_TIMEOUT_RELEASED
  { none     , start_timer_cane_released    , none                          , none                 , none              , none                         , start_timer_cane_released    , start_timer_cane_released    , start_timer_cane_released    }, // EV_NOT_PRESSED
  { none     , alarm_collision_right_on     , none                          , none                 , none              , none                         , alarm_collision_right_on     , alarm_collision_both_on      , none                         }, // EV_OBSTACLE_RIGHT
  { none     , none                         , none                          , none                 , none              , none                         , alarm_collision_off          , none                         , alarm_collision_left_on      }, // EV_NO_OBSTACLE_RIGHT
  { none     , alarm_collision_left_on      , none                          , none                 , none              , none                         , alarm_collision_both_on      , alarm_collision_left_on      , none                         }, // EV_OBSTACLE_LEFT
  { none     , none                         , none                          , none                 , none              , none                         , none                         , alarm_collision_off          , alarm_collision_right_on     }, // EV_NO_OBSTACLE_LEFT
  { error    , error                        , error                         , error                , error             , error                        , error                        , error                        , error                        }  // EV_TIMEOUT
};

/****************************************************** 
                    Global Variables 
******************************************************/
bool timeout;
long lct;

bool timer_cane_released_initiated = false;
long time_cane_released = 0;

int sound_switch_value;

bool previous_value_power_on_button_reset_needed = false;
bool previous_value_sound_button_reset_needed = false;
bool previous_value_preassure_sensor_reset_needed = false;
bool previous_value_distance_sensor_reset_needed = false;



/******************************************************
                    WiFi y MQTT
******************************************************/

typedef enum
{
  CONNECTION_UNKNOWN       = -1,
  CONNECTION_DISCONNECTED  =  0,
  CONNECTION_CONNECTED     =  1
}connection_state_t;



const char* ssid     = "SO Avanzados";
const char* password = "SOA.2019";

// Data fot EMQX
char mqtt_server_emqx[CREDENTIALS_LENGTH] = "broker.emqx.io";
char user_name_emqx[CREDENTIALS_LENGTH]   = "";
char user_pass_emqx[CREDENTIALS_LENGTH]   = "";

// Topics
char topic_cmd[TOPIC_NAME_LENGTH];
char topic_on_off_status[TOPIC_NAME_LENGTH];
char topic_obstacle_status[TOPIC_NAME_LENGTH];
char topic_alarm_status[TOPIC_NAME_LENGTH];

// Variables used internally
const char* mqtt_server; 

char user_name[CREDENTIALS_LENGTH];
char user_pass[CREDENTIALS_LENGTH];

int mqtt_port = MQTT_PORT;
WiFiClient espClient;
PubSubClient client(espClient);
char clientId[CREDENTIALS_LENGTH];

int mqtt_command = INITIAL_MQTT_COMMAND;
int mqtt_previous_command = INITIAL_MQTT_COMMAND;

connection_state_t mqtt_connection          = CONNECTION_UNKNOWN;
connection_state_t mqtt_previous_connection = CONNECTION_UNKNOWN;

wl_status_t wifi_connection           = (wl_status_t)(CONNECTION_UNKNOWN);
wl_status_t wifi_previous_connection  = (wl_status_t)(CONNECTION_UNKNOWN);



/****************************************************** 
          FreeRTOS Tasks: Structs and variables
******************************************************/
volatile int left_distance;
volatile int right_distance;

typedef enum 
{
  ACTUATOR_OFF,
  ACTUATOR_OBSTACLE_PATTERN,
  BUZZER_ALARM_PATTERN
} ActuatorCommands;

typedef struct 
{
  int toneHZ;
  int iterations;
  ActuatorCommands command;
} BuzzerMessage;

typedef struct 
{
  int iterations;
  ActuatorCommands command;
} VibrationMotorMenssage;

QueueHandle_t xQueueBuzzer;
QueueHandle_t xQueueVibrationMotor;



/****************************************************** 
          Buzzer task and auxiliar functions
******************************************************/
void execute_obstacle_pattern(int &toneHZ, int &iterations, ActuatorCommands &mode) 
{
  for (int i = 0; i < iterations; i++)
  {
    ActuatorCommands new_mode;
    int new_tone; 
    int new_iterations;

    if (process_buzzer_message(new_mode, new_tone, new_iterations, NO_WAIT)) 
    {
      if(mode != new_mode || toneHZ != new_tone || iterations != new_iterations)
      {
        mode = new_mode;
        toneHZ = new_tone;
        iterations = new_iterations;
        return;
      }
    }

    tone(PIN_BUZZER, toneHZ);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
    noTone(PIN_BUZZER);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
  }

  vTaskDelay(pdMS_TO_TICKS(LONG_DELAY_TASK));
}  

void execute_alarm_pattern(int &toneHZ, int &iterations, ActuatorCommands &mode) 
{
  for (int i = 0; i < iterations; i++)
  {
    ActuatorCommands new_mode;
    int new_tone;
    int new_iterations;

    if (process_buzzer_message(new_mode, new_tone, new_iterations, NO_WAIT)) 
    {
      if(mode != new_mode || toneHZ != new_tone || iterations != new_iterations)
      {
        mode = new_mode;
        toneHZ = new_tone;
        iterations = new_iterations;
        return;
      }
    }

    tone(PIN_BUZZER, toneHZ);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
    noTone(PIN_BUZZER);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
  }

  vTaskDelay(pdMS_TO_TICKS(LONG_DELAY_TASK));
}

bool process_buzzer_message(ActuatorCommands &mode, int &toneHZ, int &iterations, TickType_t timeout) 
{
  BuzzerMessage message; 
  if (xQueueReceive(xQueueBuzzer, &message, timeout) == pdTRUE)
  {
    mode = message.command;
    toneHZ = message.toneHZ;
    iterations = message.iterations;

    return true;
  }
  return false;
}

// Task 1: Buzzer
void buzzer_task(void *parameter)
{
  ActuatorCommands current_mode = ACTUATOR_OFF;
  int current_tone = 0;  
  int iterations = 0;

  while (true)
  {
    vTaskDelay(pdMS_TO_TICKS(SMALL_DELAY));
    if (current_mode == ACTUATOR_OFF)
    {
      process_buzzer_message(current_mode, current_tone, iterations, portMAX_DELAY);
    } else
    {
      process_buzzer_message(current_mode, current_tone, iterations, NO_WAIT);
    }

    switch (current_mode) 
    {
      case ACTUATOR_OBSTACLE_PATTERN:
        execute_obstacle_pattern(current_tone, iterations, current_mode);
        break;

      case BUZZER_ALARM_PATTERN:
        execute_alarm_pattern(current_tone, iterations, current_mode);
        break;

      case ACTUATOR_OFF:
      default:
        noTone(PIN_BUZZER);
        break;
    }
  }
}

/****************************************************** 
      Vibration Motor task and auxiliar functions
******************************************************/
bool process_motor_message(ActuatorCommands &mode, int &iterations, TickType_t timeout)
{
  VibrationMotorMenssage message;
  if (xQueueReceive(xQueueVibrationMotor, &message, timeout) == pdTRUE)
  {
    mode = message.command;
    iterations = message.iterations;
    return true;
  }
  return false;
}
  
void execute_obstacle_motor_pattern(int &iterations, ActuatorCommands &mode)
{
  for (int i = 0; i < iterations; i++)
  {
    ActuatorCommands new_mode;
    int new_iteration_value;

    if (process_motor_message(new_mode, new_iteration_value, NO_WAIT)) 
    {
      if(mode != new_mode || iterations != new_iteration_value)
      {
        mode = new_mode;
        iterations = new_iteration_value;
        return;
      }
    }

    digitalWrite(PIN_VIBRATION_MOTOR, HIGH);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
    digitalWrite(PIN_VIBRATION_MOTOR, LOW);
    vTaskDelay(pdMS_TO_TICKS(DELAY_TASK));
  }
  vTaskDelay(pdMS_TO_TICKS(LONG_DELAY_TASK));
}

// Task 2: Vibration Motor
void motor_task(void *parameter)
{
  ActuatorCommands current_mode = ACTUATOR_OFF;
  int iterations = 0;
  
  while (true)
  {
    vTaskDelay(pdMS_TO_TICKS(SMALL_DELAY));
    if (current_mode == ACTUATOR_OFF)
    {
      process_motor_message(current_mode, iterations, portMAX_DELAY);
    } else {
      process_motor_message(current_mode, iterations, NO_WAIT);
    }

    switch (current_mode)
    {
      case ACTUATOR_OBSTACLE_PATTERN:
        execute_obstacle_motor_pattern(iterations, current_mode);
        break;

      case ACTUATOR_OFF:
      default:
        digitalWrite(PIN_VIBRATION_MOTOR, LOW);
        break;
    }
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) 
{
  if (strcmp(topic, topic_cmd) == 0) 
  {
    String messageTemp;
    for (unsigned int i = 0; i < length; i++) 
    {
      messageTemp += (char)payload[i];
    }
    messageTemp.trim();

    DebugPrint("Comando MQTT recibido: ");
    DebugPrintln(messageTemp);

    if (messageTemp.equalsIgnoreCase("ON")) 
    {
      mqtt_command = ON;
    } 
    else if (messageTemp.equalsIgnoreCase("OFF")) 
    {
      mqtt_command = OFF;
    }
  }
}


// Task 3: mensajes mqtt
void mqtt_task(void *parameter)
{
  while (true)
  {
    client.loop();
    vTaskDelay(pdMS_TO_TICKS(SMALL_DELAY));
  }
}




/****************************************************** 
                      Setup Function 
******************************************************/
void do_init()
{
  Serial.begin(9600);

  pinMode(PIN_BUZZER, OUTPUT);
  
  pinMode(PIN_LED_GREEN, OUTPUT);
  pinMode(PIN_VIBRATION_MOTOR , OUTPUT);
  pinMode(PIN_LED_RED , OUTPUT);

  buttons[POWER_ON_BUTTON].pin=PIN_BUTTON_ON;
  buttons[ALARM_SOUND_BUTTON].pin=PIN_SOUND_BUTTON;

  pinMode(PIN_SOUND_BUTTON , INPUT_PULLUP);
  pinMode(PIN_BUTTON_ON , INPUT_PULLUP);

  analog_sensors[PRESSURE_SENSOR].pin = PIN_PRESSURE_SENSOR;
  sensores_distancia[LEFT_DISTANCE_SENSOR].pin_echo = PIN_ECHO_LEFT_DISTANCE_SENSOR;
  sensores_distancia[LEFT_DISTANCE_SENSOR].pin_trig = PIN_TRIG_LEFT_DISTANCE_SENSOR;
  sensores_distancia[RIGHT_DISTANCE_SENSOR].pin_echo = PIN_ECHO_RIGHT_DISTANCE_SENSOR;
  sensores_distancia[RIGHT_DISTANCE_SENSOR].pin_trig = PIN_TRIG_RIGHT_DISTANCE_SENSOR;

  pinMode(sensores_distancia[LEFT_DISTANCE_SENSOR].pin_trig , OUTPUT);
  pinMode(sensores_distancia[LEFT_DISTANCE_SENSOR].pin_echo , INPUT);
  pinMode(sensores_distancia[RIGHT_DISTANCE_SENSOR].pin_echo , INPUT);
  pinMode(sensores_distancia[RIGHT_DISTANCE_SENSOR].pin_trig , OUTPUT);
  
  xQueueBuzzer = xQueueCreate(QUEUE_SIZE, sizeof(BuzzerMessage));
  xQueueVibrationMotor = xQueueCreate(QUEUE_SIZE, sizeof(VibrationMotorMenssage));

  xTaskCreate(
    buzzer_task,
    "buzzer_task",
    BUZZER_TASK_STACK_SIZE ,
    NULL,
    BUZZER_TASK_PRIORITY,
    NULL
  );

  xTaskCreate(
    motor_task,
    "motor_task",
    MOTOR_TASK_STACK_SIZE,
    NULL,
    MOTOR_TASK_PRIORITY,
    NULL
  );

  xTaskCreate(
    mqtt_task,
    "mqtt_task",
    MQTT_TASK_STACK_SIZE ,
    NULL,
    MQTT_TASK_PRIORITY,
    NULL
  );

  shut_down_system();
  current_state = ST_OFF;
  DebugPrintEstado(current_state, new_event);
  
  timeout = false;
  lct = millis();
  
  buttons[POWER_ON_BUTTON].previous_value = INITIAL_SENSOR_VALUE;
  buttons[ALARM_SOUND_BUTTON].previous_value = INITIAL_SENSOR_VALUE;
  analog_sensors[PRESSURE_SENSOR].previous_value = INITIAL_SENSOR_VALUE;

  randomSeed(analogRead(0));
  define_broker();
  init_wifi();
  init_mqtt();
}

/****************************************************** 
                    Sensors Functions 
******************************************************/
long read_pressure_sensor()
{
  return analogRead(PIN_PRESSURE_SENSOR);
}

long read_button_on()
{
  return digitalRead(PIN_BUTTON_ON);
}

bool verify_button_on()
{
  buttons[POWER_ON_BUTTON].current_value = read_button_on();
  
  int current_value = buttons[POWER_ON_BUTTON].current_value;
  int previous_value = buttons[POWER_ON_BUTTON].previous_value;

  if( current_value != previous_value)
  {
    buttons[POWER_ON_BUTTON].previous_value = current_value;

    if(current_value == ON)
    {
      new_event = EV_SWITCH_ON;

    } else
    {
      new_event = EV_SWITCH_OFF;
    }
    return true;
  }

  return false;
}

long read_sound_button()
{
  return digitalRead(PIN_SOUND_BUTTON);
}

bool verify_sound_button()
{
  buttons[ALARM_SOUND_BUTTON].current_value = read_sound_button();
  sound_switch_value = buttons[ALARM_SOUND_BUTTON].current_value;
  
  int current_value = buttons[ALARM_SOUND_BUTTON].current_value;
  int previous_value = buttons[ALARM_SOUND_BUTTON].previous_value;

  if( current_value != previous_value)
  {
    buttons[ALARM_SOUND_BUTTON].previous_value = current_value;
    
    if(current_value == ON)
    {
        new_event = EV_SWITCH_SOUND_ON;

    } else
    {
      new_event = EV_SWITCH_SOUND_OFF;
    }
    return true;
  }

  return false;
}

bool verify_pressure_sensor()
{
  analog_sensors[PRESSURE_SENSOR].current_value = read_pressure_sensor();
  
  int current_value = analog_sensors[PRESSURE_SENSOR].current_value;
  int previous_value= analog_sensors[PRESSURE_SENSOR].previous_value;

  if( current_value != previous_value)
  {
    analog_sensors[PRESSURE_SENSOR].previous_value = current_value;
    
    if( current_value < PRESSUSURE_THRESHOLD)
    {
      new_event = EV_NOT_PRESSED;
    } else
    {
      new_event = EV_PRESSED;
    }  
    return true;
  }  
  return false;
}

long read_ultrasonic_sensor(stSensorDist sensor_dist)
{
  int triggerPin = sensor_dist.pin_trig;
  int echoPin = sensor_dist.pin_echo;

  digitalWrite(triggerPin, LOW);
  delayMicroseconds(TIME_ULTRASONIC_2);
  
  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(TIME_ULTRASONIC_10);
  
  digitalWrite(triggerPin, LOW);

  return pulseIn(echoPin, HIGH)* SOUND_SPEED/2;
}

bool verify_left_distance_sensor()
{
  int current_value = read_ultrasonic_sensor(sensores_distancia[LEFT_DISTANCE_SENSOR]);
  left_distance = current_value;

  int previous_value = sensores_distancia[LEFT_DISTANCE_SENSOR].previous_value;
  
  if( current_value != previous_value)
  {
    sensores_distancia[LEFT_DISTANCE_SENSOR].previous_value = current_value;
    
    if( current_value < DISTANCE_THRESHOLD)
    {
      new_event = EV_OBSTACLE_LEFT;
    } else
    {
      new_event = EV_NO_OBSTACLE_LEFT;
    }  
    return true;
  }
  
  return false;
}

bool verify_right_distance_sensor()
{
  int current_value = read_ultrasonic_sensor(sensores_distancia[RIGHT_DISTANCE_SENSOR]);
  right_distance = current_value;
  
  int previous_value = sensores_distancia[RIGHT_DISTANCE_SENSOR].previous_value;
  
  if( current_value != previous_value)
  {
    sensores_distancia[RIGHT_DISTANCE_SENSOR].previous_value = current_value;
    
    if( current_value < DISTANCE_THRESHOLD)
    {
      new_event = EV_OBSTACLE_RIGHT;
    } else
    {
      new_event = EV_NO_OBSTACLE_RIGHT;
    }
    return true;
  }
  
  return false;
}

bool verify_timer_released()
{
  int elapsed_time = millis() - time_cane_released;
  if(timer_cane_released_initiated == true && elapsed_time >= TIMEOUT_CANE_RELEASED)
  {
    timer_cane_released_initiated = false;
    new_event = EV_TIMEOUT_RELEASED;
    return true;
  }

  return false;
}

/****************************************************** 
                      Actuators Functions 
******************************************************/
 void turn_on_green_led()
{
  digitalWrite(PIN_LED_GREEN, true);
  digitalWrite(PIN_VIBRATION_MOTOR , false);
  digitalWrite(PIN_LED_RED , false);
}

void turn_on_red_led()
{
  digitalWrite(PIN_LED_GREEN, false);
  digitalWrite(PIN_VIBRATION_MOTOR , false);
  digitalWrite(PIN_LED_RED , true);
}

void turn_off_leds()
{
  digitalWrite(PIN_LED_GREEN, false);
  digitalWrite(PIN_VIBRATION_MOTOR , false);
  digitalWrite(PIN_LED_RED , false);
}

void turn_motor_on()
{
  VibrationMotorMenssage message;
  get_vibration_motor_pattern(left_distance, right_distance, &message.iterations);
  message.command = ACTUATOR_OBSTACLE_PATTERN;

  xQueueOverwrite(xQueueVibrationMotor, &message);
}

void turn_off_vibration_motor()
{
  VibrationMotorMenssage message;
  message.iterations = OFF;
  message.command = ACTUATOR_OFF; 

  xQueueOverwrite(xQueueVibrationMotor, &message);
}

void turn_on_buzzer(){
  BuzzerMessage message;
  get_buzzer_pattern(left_distance, right_distance, &message.toneHZ, &message.iterations);
  message.command = ACTUATOR_OBSTACLE_PATTERN;

  xQueueOverwrite(xQueueBuzzer, &message);
}

void turn_off_buzzer()
{
  BuzzerMessage message;
  message.toneHZ = OFF;           
  message.iterations = OFF;         
  message.command = ACTUATOR_OFF; 

  xQueueOverwrite(xQueueBuzzer, &message);
}

/****************************************************** 
                WiFi and MQTT Functions 
******************************************************/
void define_broker() 
{
    mqtt_server = mqtt_server_emqx;

    strcpy(user_name, user_name_emqx);
    strcpy(user_pass, user_pass_emqx);

    strcpy(topic_on_off_status, TOPIC_MQTT_ON_OFF_EMQX);
    strcpy(topic_cmd, TOPIC_MQTT_CMD_EMQX);
    strcpy(topic_obstacle_status, TOPIC_MQTT_OBSTACLE_STATUS_EMQX);
    strcpy(topic_alarm_status, TOPIC_MQTT_ALARM_STATUS_EMQX);
}


void mqttConnect()
{
  if (!client.connected()) 
  {
    DebugPrintln("Conectando al broker MQTT...");
    long r = random(1000);
    sprintf(clientId, "clientSmartCaneId-%ld", r);
    if (client.connect(clientId, user_name, user_pass))
    {
      DebugPrintln(" ¡Conectado!");
      client.subscribe(topic_cmd);
    } else 
    {
      DebugPrint("Error MQTT rc=");
      DebugPrintln(client.state());
    }
  }
}


void wifiConnect()
{
  WiFi.begin(ssid, password);
  if (WiFi.status() == WL_CONNECTED)
  {
    DebugPrintln(" ¡Conectado a wifi!");
    DebugPrint("IP: ");
    DebugPrintln(WiFi.localIP());
  }
  else
  {
    DebugPrintln("No se pudo establecer la conexion wifi");
  } 
}


void set_active_state()
{
  current_state = ST_ACTIVE;
  DebugPrintEstado(current_state, new_event);
}

void set_wifi_disconnected_state()
{
  shut_down_system();
  current_state = ST_WIFI_DISCONNECTED;
  DebugPrintEstado(current_state, new_event);
}

void set_mqtt_disconnected_state()
{
  shut_down_system();
  current_state = ST_MQTT_DISCONNECTED;
  DebugPrintEstado(current_state, new_event);
}


bool verify_mqtt_command()
{
  if( mqtt_command != mqtt_previous_command)
  {
    mqtt_previous_command = mqtt_command;
    if(mqtt_command == ON)
    {
      new_event = EV_SWITCH_ON;

    } else
    {
      new_event = EV_SWITCH_OFF;
    }
    return true;
  }

  return false;
}


bool verify_mqtt_connection()
{
  mqtt_connection = client.connected() ? CONNECTION_CONNECTED : CONNECTION_DISCONNECTED;

  if(mqtt_connection == CONNECTION_CONNECTED)
  {
    new_event = EV_MQTT_DISCONNECTED;

  } else
  {
    new_event = EV_MQTT_CONNECTED;
  }
  
  return true;
}

bool verify_wifi_connection()
{
  wifi_connection = WiFi.status();
 
  if (wifi_connection == WL_CONNECTED)
  {
    new_event = EV_WIFI_CONNECTED;
  }
  else
  {
    new_event = EV_WIFI_DISCONNECTED;
  }
  return true;
}

void publishObstacleStatus(const char* status) 
{
  if (client.connected()) 
  {
    client.publish(topic_obstacle_status, status);
    DebugPrint("Estado de Obstaculo enviado: ");
    DebugPrintln(status);
  }
}

void publishReleasedCaneStatus(const char* status) 
{
  if (client.connected()) 
  {
    client.publish(topic_alarm_status, status);
    DebugPrint("Estado de Alarma enviado: ");
    DebugPrintln(status);
  }
}

void publishON_OFFStatus(const char* status) 
{
  if (client.connected()) 
  {
    client.publish(topic_on_off_status, status);
    DebugPrint("Estado de Encendido enviado: ");
    DebugPrintln(status);
  }
}

/****************************************************** 
                Get event function 
******************************************************/
void reset_sensors()
{
  if (previous_value_power_on_button_reset_needed == true)
  {
    buttons[POWER_ON_BUTTON].previous_value = INITIAL_SENSOR_VALUE;
    previous_value_power_on_button_reset_needed = false;
  }

  if (previous_value_sound_button_reset_needed == true)
  {
    buttons[ALARM_SOUND_BUTTON].previous_value = INITIAL_SENSOR_VALUE;
    previous_value_sound_button_reset_needed = false;
  }

  if (previous_value_preassure_sensor_reset_needed == true)
  {
    analog_sensors[PRESSURE_SENSOR].previous_value = INITIAL_SENSOR_VALUE;
    previous_value_preassure_sensor_reset_needed = false;
  }
  
  if (previous_value_distance_sensor_reset_needed == true)
  {
    sensores_distancia[RIGHT_DISTANCE_SENSOR].previous_value=INITIAL_SENSOR_VALUE;
    sensores_distancia[LEFT_DISTANCE_SENSOR].previous_value=INITIAL_SENSOR_VALUE;
    previous_value_distance_sensor_reset_needed = false;
  }
}

void get_new_event()
{
  long ct = millis();
  int  diference = (ct - lct);
  timeout = (diference > DIFFERENCE_THRESHOLD_TIMEOUT) ? (true) : (false);

  reset_sensors();
  
  if( timeout )
  {
    timeout = false;
    lct     = ct;
    
    if(  (verify_wifi_connection() == true) || (verify_mqtt_connection() == true) 
      || (verify_mqtt_command() == true) || (verify_button_on() == true) || ( verify_pressure_sensor() == true) 
      || (verify_timer_released() == true) ||  (verify_sound_button() == true)
      || (verify_left_distance_sensor() == true) ||  (verify_right_distance_sensor() == true) )
    {
      return;
    }
  }
  
  new_event = EV_CONT;
}

/****************************************************** 
                Transition Functions 
******************************************************/
void error()
{
}

void none()
{
}

int determine_tone(int distance)
{
  if (distance <= DISTANCE_VERY_CLOSE) return TONE_BUZZER_HIGH;
  if (distance <= DISTANCE_CLOSE) return TONE_BUZZER_MEDIUM;
  
  return TONE_BUZZER_LOW;
}

void get_buzzer_pattern(int left_distance, int right_distance, int *toneHZ, int *iterations)
{
  int min_distance = min(left_distance, right_distance);

  if (left_distance <= DISTANCE_MEDIUM && right_distance > DISTANCE_MEDIUM) 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_LEFT;
    *toneHZ = determine_tone(left_distance);
  } 
  else if (right_distance <= DISTANCE_MEDIUM && left_distance > DISTANCE_MEDIUM) 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_RIGHT;
    *toneHZ = determine_tone(right_distance);
  } 
  else 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_BOTH;
    *toneHZ = determine_tone(min_distance);
  }
}

void get_vibration_motor_pattern(int left_distance, int right_distance, int *iterations)
{
  int min_distance = min(left_distance, right_distance); 

  if (left_distance <= DISTANCE_MEDIUM && right_distance > DISTANCE_MEDIUM) 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_LEFT;
  } 
  else if (right_distance <= DISTANCE_MEDIUM && left_distance > DISTANCE_MEDIUM) 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_RIGHT;
  } 
  else 
  {
    *iterations = ITERATIONS_OBSTACLE_ALARM_BOTH;
  }
}

void alarm_collision_off()
{
  turn_off_vibration_motor();
  turn_off_buzzer();
  current_state = ST_ACTIVE;

  DebugPrintEstado(current_state, new_event);

  publishObstacleStatus("NO_OBSTACLE");
}

void create_message_buzzer()
{
 if(sound_switch_value == ON)
 {
    BuzzerMessage message;
    get_buzzer_pattern(left_distance, right_distance, &message.toneHZ, &message.iterations);
    message.command = ACTUATOR_OBSTACLE_PATTERN;
    xQueueOverwrite(xQueueBuzzer, &message);
 }
}

void alarm_collision_right_on()
{
  turn_motor_on();
  create_message_buzzer();
  current_state = ST_RIGHT_OBSTACLE;

  DebugPrintEstado(current_state, new_event);
  
  publishObstacleStatus("OBSTACLE_RIGHT");
}

void alarm_collision_left_on()
{
  turn_motor_on();
  create_message_buzzer();
  current_state = ST_LEFT_OBSTACLE;

  DebugPrintEstado(current_state, new_event);

  publishObstacleStatus("OBSTACLE_LEFT");
}

void alarm_collision_both_on()
{
  turn_motor_on( );
  create_message_buzzer(); 
  current_state = ST_BOTH_OBSTACLE;

  DebugPrintEstado(current_state, new_event);

  publishObstacleStatus("OBSTACLE_BOTH");
}

void alarm_stick_on()
{
  turn_off_vibration_motor();
  turn_on_red_led();

  BuzzerMessage message;
  message.toneHZ = TONE_ALARM_CANE_RELEASED;
  message.iterations = ITERATIONS_ALARM_CANE_RELEASED;      
  message.command = BUZZER_ALARM_PATTERN; 
  xQueueOverwrite(xQueueBuzzer, &message);
  current_state = ST_CANE_REL;

  publishReleasedCaneStatus("CANE RELEASED");
  DebugPrintEstado(current_state, new_event);
}

void alarm_stick_off()
{
  turn_off_leds();
  turn_on_green_led();
  turn_off_buzzer();
  turn_off_vibration_motor();

  previous_value_distance_sensor_reset_needed = true;
  timer_cane_released_initiated = false;

  current_state = ST_ACTIVE;

  publishReleasedCaneStatus("CANE USE RESUMED");
  DebugPrintEstado(current_state, new_event);
}

void start_timer_cane_released()
{
  turn_off_buzzer();
  turn_off_vibration_motor();
  timer_cane_released_initiated = true;
  time_cane_released = millis();
  current_state = ST_TIMER_WAITING;

  DebugPrintEstado(current_state, new_event);
}

void turn_off()
{
  shut_down_system();

  current_state = ST_OFF;

  publishON_OFFStatus("CANE OFF");
  DebugPrintEstado(current_state, new_event);
}


void shut_down_system()
{
  turn_off_leds();
  turn_off_buzzer();
  turn_off_vibration_motor();
}

void turn_on()
{
  turn_on_green_led();
  turn_off_buzzer();
  turn_off_vibration_motor();
  
  previous_value_power_on_button_reset_needed = true;
  previous_value_sound_button_reset_needed = true;
  previous_value_preassure_sensor_reset_needed = true;
  previous_value_distance_sensor_reset_needed = true;

  current_state = ST_ACTIVE;

  publishON_OFFStatus("CANE ON");
  DebugPrintEstado(current_state, new_event);
}

void sound_alarm_obstacle_off()
{
  turn_off_buzzer();
}

void sound_alarm_obstacle_on()
{
  turn_on_buzzer();
}

void init_wifi()
{
  WiFi.begin(ssid, password);
  DebugPrintln("Intentando conexión inicial WiFi...");
}

void init_mqtt()
{
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqttCallback);
  mqttConnect();
}

/****************************************************** 
          State Machine and principal functions 
******************************************************/
void state_machine( )
{
  get_new_event();

  if( (new_event >= 0) && (new_event < MAX_EVENTS) && 
    (current_state >= 0) && (current_state < MAX_STATES) )
  {
    state_table[new_event][current_state]();
  }

  new_event = EV_CONT;
}

void setup()
{
  do_init();
}

void loop()
{
  state_machine();
}