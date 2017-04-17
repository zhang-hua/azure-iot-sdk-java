package com.iothub.azure.microsoft.com.androidsample;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{

    private static String connString = "[device connection string]";
    private static TextView messageText;
    private static ArrayList<String> communicationMessages = new ArrayList<>();
    private static DeviceClient iothubClient = null;
    private static int messageCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageText = (TextView)findViewById(R.id.messageText);
        showNewItem("START");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        startIoTHub();

        try
        {
            SendMessage(5, "RESUME");
        }
        catch(IOException e1)
        {
            showNewItem("Exception while opening IoTHub connection: " + e1.toString());
        }
        catch(Exception e2)
        {
            showNewItem("Exception while opening IoTHub connection: " + e2.toString());
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopIoTHub();
    }

    public void startIoTHub()
    {
        // Comment/uncomment from lines below to use HTTPS or MQTT protocol
        // IotHubClientProtocol protocol = IotHubClientProtocol.HTTPS;
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        try
        {
            iothubClient = new DeviceClient(connString, protocol);
            iothubClient.open();
            startReceiveMessage(protocol);
        }
        catch(Exception e1)
        {
            showNewItem("Exception while opening IoTHub connection: " + e1.toString());
        }
    }

    public void stopIoTHub()
    {
        try
        {
            iothubClient.close();
        }
        catch(Exception e1)
        {
            showNewItem("Exception while opening IoTHub connection: " + e1.toString());
        }
    }

    public void SendMessage(int numberOfMessages, String messageToSend) throws URISyntaxException, IOException
    {
        for (int i = 0; i < numberOfMessages; ++i)
        {
            SendMessage("Event Message " + messageToSend + "[" + Integer.toString(i) + "]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void SendMessage(String messageToSend) throws URISyntaxException, IOException
    {
        showNewItem("send:" + messageToSend);

        try
        {
            Message msg = new Message(messageToSend);
            msg.setProperty("messageCount", Integer.toString(messageCounter));
            EventCallback eventCallback = new EventCallback();
            iothubClient.sendEventAsync(msg, eventCallback, (messageCounter++));
        }
        catch (Exception e)
        {
            showNewItem("Failed send event: " + e.toString());
        }
    }

    public void btnSendOnClick(View v) throws URISyntaxException, IOException
    {
        Button button = (Button) v;

        SendMessage("Button \"" + button.getText() + "\" clicked!");
    }

    protected void startReceiveMessage(IotHubClientProtocol protocol) throws URISyntaxException, IOException
    {
        if (protocol == IotHubClientProtocol.MQTT)
        {
            MessageCallbackMqtt callback = new MessageCallbackMqtt();
            Counter counter = new Counter(0);
            iothubClient.setMessageCallback(callback, counter);
        }
        else
        {
            MessageCallback callback = new MessageCallback();
            Counter counter = new Counter(0);
            iothubClient.setMessageCallback(callback, counter);
        }
    }

    // Our MQTT doesn't support abandon/reject, so we will only display the messaged received
    // from IoTHub and return COMPLETE
    protected static class MessageCallbackMqtt implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            communicationMessages.add(
                    "Received MQTT message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            counter.increment();

            return IotHubMessageResult.COMPLETE;
        }
    }

    protected static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            Integer i = (Integer) context;
            showNewItem("IoTHub responded message "+i.toString()
                    + " with status " + status.name());
        }
    }

    protected static class MessageCallback implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Counter counter = (Counter) context;
            showNewItem(
                    "Received message " + counter.toString()
                            + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal)
            {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            showNewItem("Responding to message " + counter.toString() + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    /** Used as a counter in the message callback. */
    protected static class Counter
    {
        protected int num;

        public Counter(int num)
        {
            this.num = num;
        }

        public int get()
        {
            return this.num;
        }

        public void increment()
        {
            this.num++;
        }

        @Override
        public String toString()
        {
            return Integer.toString(this.num);
        }
    }

    protected static void showNewItem(String newMessage)
    {
        communicationMessages.add(newMessage);
        StringBuilder str = new StringBuilder();
        int end = communicationMessages.size();
        int start = end-20;
        if(start<0)
        {
            start = 0;
        }

        for (String message:communicationMessages.subList(start, end))
        {
            str.append(message + "\r\n");
        }

        try
        {
            messageText.setText(str);
        }
        catch (Exception e)
        {

        }
    }

}
