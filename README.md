# Este repositorio se utilizará durante la cursada de SOA

El repositorio esta conformado por tres directorios, los alumnos deberán almacenar lo desarrollado en sus actividades prácticas. A continuación se espécifica el contenido que debe tener cada directorio.

- **ANDROID:** En este directorio se debe colocar el código fuente del proyecto desarrollado en Android Studio. 
- **EMBEBIDO:** En este directorio se debe colocar el código fuente del proyecto del embebido desarrollado(ESP32, Arduino,Raspberry PI, etc.).
- **INFORMES:** En este directorio se deben colocar los informes realizados en las actividades prácticas. También aquí se puede subir material complementario de documentación del proyecto (Diagramas,imagenes, videos,entre otros)
---
# SmartCane: Bastón Inteligente para Personas con Discapacidad Visual
SmartCane es una solución tecnológica inclusiva compuesta por un bastón inteligente y una aplicación móvil, diseñada para mejorar la **movilidad**, **seguridad** y **autonomía** de personas con **discapacidad visual**.

## 👥 Integrantes del Proyecto

| Nombre completo                  | DNI       |
|----------------------------------|-----------|
| Di Nicco, Luis Demetrio          | 43664669  |
| Antonioli, Iván Oscar            | 43630151  |
| López Ferme, Nahuel Ezequiel     | 43991086  |
| Sanchez, Kevin                   | 41173649  |
| Intrieri, Gabriel Yamil          | 38128822  |

## Descripción General

El sistema SmartCane se compone de dos elementos principales:

- **SmartCane (Hardware)**: Bastón inteligente con sensores de detección de obstáculos y sistemas de alerta multisensorial.
- **SmartCane App (Software)**: Aplicación móvil Android que permite el control remoto, monitoreo y asistencia en tiempo real para el usuario no vidente y sus familiares.

## Características del Bastón

- Detección de obstáculos mediante **sensores ultrasónicos** (HC-SR04).
- **Alertas multicanal**: sonido (buzzer), vibración (motor) y luces LED.
- Detección de **dirección** (izquierda, derecha, frente) y **distancia** (tono grave, medio, agudo).
- Sensor de presión para identificar si el bastón está siendo sostenido.
- Sistema de alerta por **caída o pérdida** del bastón.
- Controles físicos:
  - **Boton Switch 1**: Encendido/apagado general.
  - **Boton Switch 2**: Silenciado del buzzer (modo silencioso).
- Basado en **ESP32 con FreeRTOS**.

## Funcionamiento General

1. Al encender el bastón, todos los sensores y sistemas quedan habilitados.
2. Se detectan obstáculos en tres direcciones: izquierda, derecha y frente.
3. Se alerta al usuario mediante:
   - **Buzzer** (con tonos distintos según cercanía)
   - **Motor vibrador** (patrones distintos según dirección)
   - **Luces LED** (verde para sistema activo, rojo para caída)
4. Si el usuario suelta el bastón:
   - Se activa una alerta (buzzer + LED rojo).
   - Se notifica automáticamente a la aplicación SmartCane App con la ubicación del usuario.
  
## Descripción de los Estados del Sistema SmartCane

| Estado                  | Descripción                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| `ST_OFF`               | Estado inicial o apagado del sistema. No se ejecutan acciones excepto cuando se enciende. |
| `ST_ACTIVE`            | Estado activo del sistema, donde está operativo y puede interactuar con sensores/red. |
| `ST_MQTT_DISCONNECTED` | Indica que la conexión MQTT está caída. El sistema puede intentar reconectarse. |
| `ST_WIFI_DISCONNECTED` | Similar al anterior, pero para desconexión WiFi. También puede intentar reconectar. |
| `ST_TIMER_WAITING`     | Estado de espera de 3 segundos tras soltar el bastón, esperando si se presiona nuevamente. |
| `ST_CANE_REL`          | El bastón ha sido soltado por más de 3 segundos. Se activa la alarma. |
| `ST_RIGHT_OBSTACLE`    | Se ha detectado un obstáculo con el sensor ultrasónico derecho. |
| `ST_LEFT_OBSTACLE`     | Se ha detectado un obstáculo con el sensor ultrasónico izquierdo. |
| `ST_BOTH_OBSTACLE`     | Se ha detectado un obstáculo con ambos sensores ultrasónicos. |

## 📱 SmartCane App - Aplicación Móvil

Una app Android complementaria para dos perfiles de usuario:

### Usuario No Vidente

- Encender/apagar el bastón de forma remota.
- Recibir alertas sobre obstáculos detectados.
- Visualización del conteo de pasos realizados durante el trayecto.
- Compatibilidad con Google Talckback para lectura de la pantalla por voz.

### Familiar

- Ver estado del bastón en tiempo real (ON/OFF, soltado, en uso).
- Recibir **alertas de emergencia** con la ubicación del usuario en caso de caída.
- Visualizar el historial de pasos del usuario para seguimiento de movilidad.

La app utiliza el protocolo **MQTT** para comunicación en tiempo real entre el bastón, el servidor y los smartphones.

## Tecnologías Utilizadas

- **ESP32 + FreeRTOS**
- **Sensores:** Sensores de distancia HC-SR04, buzzer, motor vibrador, LEDs, sensor de presión tactil
- **MQTT** (broker para comunicación IoT)
- **Android** (Java)
- **Google Maps API** (para ubicación en emergencias)
- **Reconocimiento de actividad física** (para conteo de pasos)
  
# Prototipo realizado

![SmartCane Prototipo](https://www.soa-unlam.com.ar/wiki/images/thumb/4/48/GrupoM1_Smartcane_Prototipo%281%29.jpg/450px-GrupoM1_Smartcane_Prototipo%281%29.jpg)
