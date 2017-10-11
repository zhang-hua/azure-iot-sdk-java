# IotHubReactor Requirements

## Overview

Class to handle running a proton reactor. 

## References

## Exposed API

```java
class IotHubReactor
{
    IotHubReactor(Reactor reactor, boolean usingSasl);
    void run() throws HandlerException;
}
```

### IotHubReactor

```java
IotHubReactor(Reactor reactor, boolean usingSasl);
```

**SRS_IOTHUBREACTOR_34_001: [**This constructor will save the provided reactor.**]**


### run

```java
void run();
```

**SRS_IOTHUBREACTOR_34_003: [**This function shall set the timeout of the reactor to 10 milliseconds.**]**

**SRS_IOTHUBREACTOR_34_004: [**This function shall start the reactor and have it process indefinitely and stop the reactor when it finishes.**]**

