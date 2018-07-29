#define RED 9         //Rgb diode pins.
#define GREEN 10
#define BLUE 11
#define COMMON_ANODE  //Undef if you have rgb diode with common cathode 

#define BTpin 2       //HC-05 state pin.

byte dataBuffer[4]= {0,0,0,0};              //Byte data buffer init.

void setColor(int red, int green, int blue) //Color setup function.
{
  #ifdef COMMON_ANODE
    red = 255 - red;
    green = 255 - green;
    blue = 255 - blue;
  #endif
  analogWrite(RED, red);
  analogWrite(GREEN, green);
  analogWrite(BLUE, blue);  
}

void btConnected(){
Serial.println("Bluetooth connected.");     //Interrupt for bluetooth connected state.
}

void btDisconnected(){
Serial.println("Bluetooth disconnected.");  //Interrupt for bluetooth disconnected state.
setColor(255,255,255);
}

void setup() {                              //Outputs and interrupts setup.
 pinMode(RED,OUTPUT);
 pinMode(GREEN,OUTPUT);
 pinMode(BLUE,OUTPUT);

 digitalWrite(RED,HIGH);
 digitalWrite(GREEN,HIGH);
 digitalWrite(BLUE,HIGH);

 attachInterrupt(digitalPinToInterrupt(2), btConnected, RISING);
 attachInterrupt(digitalPinToInterrupt(2),btDisconnected,FALLING);
 Serial.begin(115200);                                                //Serial communication init.
}

void loop() {
  interrupts();
  
  while(Serial.available()){
  Serial.readBytes(dataBuffer,4);
  
  if(dataBuffer[3]== 0){                                               //Buffer triggering.
  setColor((int)dataBuffer[0],(int)dataBuffer[1],(int)dataBuffer[2]);  //Setting rgb diode color. Red, green and blue values are in range 0-255.
  Serial.print("R: ");                                                 //Serial data print.
  Serial.print((int)dataBuffer[0]);
  Serial.print(" G: ");
  Serial.print((int)dataBuffer[1]);
  Serial.print(" B: ");
  Serial.print((int)dataBuffer[2]);
  Serial.print("\n");
  }
  
  dataBuffer[0]=0;                                                     //Cleaning buffer
  dataBuffer[1]=0;
  dataBuffer[2]=0;
  dataBuffer[3]=1;
}
}


