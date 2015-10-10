/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSubmitButton;

    private Button mCard0;
    private Button mCard1;
    private Button mCard2;
    private Button mCard3;
    private Button mCard4;
    private Button mCard5;
    private Button mCard6;
    private Button mCard7;
    private Button mCard8;

    private Button previous;

    private TextView myCard;
    private TextView oppCard;

    private TextView whiteLeft;
    private TextView blackLeft;

    private TextView oppScore;
    private TextView myScore;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private boolean received = false;
    private boolean sent = false;
    private boolean myTurn = false;

    private int myValue = -1;
    private int oppValue = -1;

    private int myScoreValue = 0;
    private int oppScoreValue = 0;

    private int whiteLeftCount = 4;
    private int blackLeftCount = 5;

    private enum Result {
        WIN, DRAW, LOSE, UNDEFINED
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
        return inflater.inflate(R.layout.board, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = (ListView) view.findViewById(R.id.in);
        //mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSubmitButton = (Button) view.findViewById(R.id.Submit);

        mCard0 = (Button) view.findViewById(R.id.Card_0);
        mCard1 = (Button) view.findViewById(R.id.Card_1);
        mCard2 = (Button) view.findViewById(R.id.Card_2);
        mCard3 = (Button) view.findViewById(R.id.Card_3);
        mCard4 = (Button) view.findViewById(R.id.Card_4);
        mCard5 = (Button) view.findViewById(R.id.Card_5);
        mCard6 = (Button) view.findViewById(R.id.Card_6);
        mCard7 = (Button) view.findViewById(R.id.Card_7);
        mCard8 = (Button) view.findViewById(R.id.Card_8);

        myCard = (TextView) view.findViewById(R.id.MyCard);
        oppCard = (TextView) view.findViewById(R.id.OppCard);

        whiteLeft = (TextView) view.findViewById(R.id.WhiteLeft);
        blackLeft = (TextView) view.findViewById(R.id.BlackLeft);

        oppScore = (TextView) view.findViewById(R.id.OppScore);
        myScore = (TextView) view.findViewById(R.id.MyScore);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        //mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // TODO: Create a rule explanation button and enable this function
        /*
        ruleExplanation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=F0X8JbX4Mjk")));
                Log.i("Video", "Video Playing....");
            }
        });*/

        //
        mCard0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard0.setEnabled(false);
                mCard0.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard0;
                myCard.setText("0");
                myCard.setTextColor(Color.WHITE);
                myCard.setBackgroundColor(Color.BLACK);
            }
        });

        mCard1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard1.setEnabled(false);
                mCard1.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard1;
                myCard.setText("1");
                myCard.setTextColor(Color.BLACK);
                myCard.setBackgroundColor(Color.WHITE);
            }
        });

        mCard2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard2.setEnabled(false);
                mCard2.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard2;
                myCard.setText("2");
                myCard.setTextColor(Color.WHITE);
                myCard.setBackgroundColor(Color.BLACK);
            }
        });

        mCard3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard3.setEnabled(false);
                mCard3.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard3;
                myCard.setText("3");
                myCard.setTextColor(Color.BLACK);
                myCard.setBackgroundColor(Color.WHITE);
            }
        });

        mCard4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard4.setEnabled(false);
                mCard4.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard4;
                myCard.setText("4");
                myCard.setTextColor(Color.WHITE);
                myCard.setBackgroundColor(Color.BLACK);
            }
        });

        mCard5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard5.setEnabled(false);
                mCard5.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard5;
                myCard.setText("5");
                myCard.setTextColor(Color.BLACK);
                myCard.setBackgroundColor(Color.WHITE);
            }
        });

        mCard6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard6.setEnabled(false);
                mCard6.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard6;
                myCard.setText("6");
                myCard.setTextColor(Color.WHITE);
                myCard.setBackgroundColor(Color.BLACK);
            }
        });

        mCard7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard7.setEnabled(false);
                mCard7.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard7;
                myCard.setText("7");
                myCard.setTextColor(Color.BLACK);
                myCard.setBackgroundColor(Color.WHITE);
            }
        });

        mCard8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCard8.setEnabled(false);
                mCard8.setBackgroundColor(Color.GRAY);
                if (previous != null) {
                    int previousId = Integer.parseInt(previous.getText().toString());
                    if (previousId % 2 == 0) {
                        previous.setBackgroundColor(Color.BLACK);
                    }
                    else {
                        previous.setBackgroundColor(Color.WHITE);
                    }
                    previous.setEnabled(true);
                }
                previous = mCard8;
                myCard.setText("8");
                myCard.setTextColor(Color.WHITE);
                myCard.setBackgroundColor(Color.BLACK);
            }
        });

        // Initialize the send button with a listener that for click events
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                if (myCard.getText().toString() == "") {
                    Toast.makeText(activity, "Please select a card", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendMessage(myCard.getText().toString());
                    sent = true;
                    myValue = Integer.parseInt(myCard.getText().toString());
                    if (myValue == 0) {
                        mCard0.setVisibility(View.GONE);
                    } else if (myValue == 1) {
                        mCard1.setVisibility(View.GONE);
                    } else if (myValue == 2) {
                        mCard2.setVisibility(View.GONE);
                    } else if (myValue == 3) {
                        mCard3.setVisibility(View.GONE);
                    } else if (myValue == 4) {
                        mCard4.setVisibility(View.GONE);
                    } else if (myValue == 5) {
                        mCard5.setVisibility(View.GONE);
                    } else if (myValue == 6) {
                        mCard6.setVisibility(View.GONE);
                    } else if (myValue == 7) {
                        mCard7.setVisibility(View.GONE);
                    } else if (myValue == 8) {
                        mCard8.setVisibility(View.GONE);
                    }
                    mSubmitButton.setEnabled(false);

                    controlButtonEnability(false);
                }
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    private void setFirstConnection() {
        //TODO: Reset all the scores along with all the buttons
        // Resetting all buttons to be Enabled
        mSubmitButton.setEnabled(true);
        mCard0.setEnabled(true);
        mCard1.setEnabled(true);
        mCard2.setEnabled(true);
        mCard3.setEnabled(true);
        mCard4.setEnabled(true);
        mCard5.setEnabled(true);
        mCard6.setEnabled(true);
        mCard7.setEnabled(true);
        mCard8.setEnabled(true);

        // Resetting all buttons' visibility to be Visible
        mCard0.setVisibility(View.VISIBLE);
        mCard1.setVisibility(View.VISIBLE);
        mCard2.setVisibility(View.VISIBLE);
        mCard3.setVisibility(View.VISIBLE);
        mCard4.setVisibility(View.VISIBLE);
        mCard5.setVisibility(View.VISIBLE);
        mCard6.setVisibility(View.VISIBLE);
        mCard7.setVisibility(View.VISIBLE);
        mCard8.setVisibility(View.VISIBLE);

        myScoreValue = 0;
        oppScoreValue = 0;
        blackLeftCount = 5;
        whiteLeftCount = 4;

        whiteLeft.setText("Left: " + whiteLeftCount);
        blackLeft.setText("Left: " + blackLeftCount);

        myScore.setText("My Score: " + myScoreValue);
        oppScore.setText("Opp Score: " + oppScoreValue);

        myCard.setText("");

        myCard.setBackgroundColor(Color.LTGRAY);
        oppCard.setBackgroundColor(Color.LTGRAY);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mConnectedDeviceName == null) {
                mSubmitButton.setEnabled(false);
                controlButtonEnability(false);

            }
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            myTurn = true;
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    Handler writeHandler = new Handler();
                    writeHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (sent && received) {
                                calculateResult();
                            }
                        }
                    }, 1500);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    received = true;
                    // TODO: change turn according to the game calculation result (if my value is set: i.e. cards are all set)
                    myTurn = !myTurn;
                    mSubmitButton.setEnabled(myTurn);
                    controlButtonEnability(myTurn);
                    //TODO: ====================THis is SUPER IMPORTANT SHIT==============
                    int receivedValue = Integer.parseInt(readMessage);
                    if (receivedValue % 2 == 0) {
                        oppCard.setBackgroundColor(Color.BLACK);
                        blackLeftCount--;
                        blackLeft.setText("Left: " + blackLeftCount);
                    }
                    else {
                        oppCard.setBackgroundColor(Color.WHITE);
                        whiteLeftCount--;
                        whiteLeft.setText("Left: " + whiteLeftCount);
                    }
                    oppValue = receivedValue;

                    Handler readHandler = new Handler();
                    readHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (sent && received) {
                                calculateResult();
                            }
                        }
                    }, 1500);

                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    setFirstConnection();
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        if (myTurn) {
                            Toast.makeText(activity, "My turn: First", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(activity, "My turn: Second", Toast.LENGTH_SHORT).show();
                            mSubmitButton.setEnabled(false);
                        }

                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }


    // Function that disables the buttons if it is not my turn + enables buttons if it is my turn
    private void controlButtonEnability(boolean isEnabled) {
        mCard0.setEnabled(isEnabled);
        mCard1.setEnabled(isEnabled);
        mCard2.setEnabled(isEnabled);
        mCard3.setEnabled(isEnabled);
        mCard4.setEnabled(isEnabled);
        mCard5.setEnabled(isEnabled);
        mCard6.setEnabled(isEnabled);
        mCard7.setEnabled(isEnabled);
        mCard8.setEnabled(isEnabled);
    }

    private void calculateResult() {
        sent = false;
        received = false;
        if (oppValue > myValue) { // YOU WIN!
            Toast.makeText(getActivity(), "You Lost!", Toast.LENGTH_SHORT).show();
            oppScoreValue++;
            oppScore.setText("Opp Score: " + oppScoreValue);
            myTurn = false;
        }
        else if (oppValue < myValue){ // I WIN!
            Toast.makeText(getActivity(), "You Won!", Toast.LENGTH_SHORT).show();
            myScoreValue++;
            myScore.setText("My Score: " + myScoreValue);
            myTurn = true;
            mSubmitButton.setEnabled(true);
        }
        else { // Draw Case
            Toast.makeText(getActivity(), "Draw...", Toast.LENGTH_SHORT).show();
            myTurn = !myTurn;
            mSubmitButton.setEnabled(myTurn);
        }
        oppCard.setBackgroundColor(Color.LTGRAY);
        myCard.setBackgroundColor(Color.LTGRAY);
        myCard.setText("");

        // Exiting the game if both the left numbers are zero
        int totalCardLeft = blackLeftCount + whiteLeftCount;
        if (((blackLeftCount == 0) && (whiteLeftCount == 0)) || (oppScoreValue > myScoreValue +totalCardLeft) || (myScoreValue > oppScoreValue +totalCardLeft)) {
            if ((myScoreValue > oppScoreValue +totalCardLeft)) {
                Toast.makeText(getActivity(), "I AM THE WINNER!!! WINNER WINNER CHICKEN DINNER!!!", Toast.LENGTH_SHORT).show();
                setFirstConnection();
                myTurn = true;
                mSubmitButton.setEnabled(myTurn);
            }
            else if ((oppScoreValue > myScoreValue + totalCardLeft)) {
                Toast.makeText(getActivity(), "I am such a loser.... Big Bang Loser", Toast.LENGTH_SHORT).show();
                setFirstConnection();
                myTurn = false;
                mSubmitButton.setEnabled(myTurn);
            }
            else if (oppScoreValue == myScoreValue){
                Toast.makeText(getActivity(), "This is a draw game", Toast.LENGTH_SHORT).show();
                setFirstConnection();

                mSubmitButton.setEnabled(myTurn);
                controlButtonEnability(myTurn);
            }
        }

        controlButtonEnability(myTurn);
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
