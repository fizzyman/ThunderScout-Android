/*
 * MIT License
 *
 * Copyright (c) 2016 - 2017 Luke Myers (FRC Team 980 ThunderBots)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.team980.thunderscout.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.team980.thunderscout.data.ScoutData;
import com.team980.thunderscout.data.task.ScoutDataWriteTask;
import com.team980.thunderscout.feed.EntryOperationWrapper;
import com.team980.thunderscout.feed.EntryOperationWrapper.EntryOperationStatus;
import com.team980.thunderscout.feed.EntryOperationWrapper.EntryOperationType;
import com.team980.thunderscout.feed.FeedEntry;
import com.team980.thunderscout.feed.task.FeedDataWriteTask;
import com.team980.thunderscout.match.ScoutingFlowActivity;
import com.team980.thunderscout.util.TSNotificationBuilder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ServerConnectionTask extends AsyncTask<Void, Integer, ScoutData> {

    private final BluetoothSocket mmSocket;

    private Context context;

    private TSNotificationBuilder notificationManager;

    public ServerConnectionTask(BluetoothSocket socket, Context context) {
        mmSocket = socket;

        this.context = context;

        notificationManager = TSNotificationBuilder.getInstance(context);
    }

    @Override
    protected void onPreExecute() {
        //Runs on UI thread before execution
        super.onPreExecute();
    }

    @Override
    protected ScoutData doInBackground(Void[] params) {
        int notificationId = notificationManager.showBtTransferInProgress(mmSocket.getRemoteDevice().getName(), true);

        ObjectInputStream fromScoutStream;
        ObjectOutputStream toScoutStream;

        try {
            toScoutStream = new ObjectOutputStream(mmSocket.getOutputStream());
            toScoutStream.flush();
            fromScoutStream = new ObjectInputStream(mmSocket.getInputStream());
        } catch (IOException e) {
            FirebaseCrash.report(e);
            notificationManager.showBtTransferError(mmSocket.getRemoteDevice().getName(),
                    notificationId);
            return null;
        }

        //TODO version check
        ScoutData data = null;
        try {
            data = (ScoutData) fromScoutStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            FirebaseCrash.report(e);
            notificationManager.showBtTransferError(mmSocket.getRemoteDevice().getName(),
                    notificationId);
            return null;
        }

        try {
            fromScoutStream.close();
            toScoutStream.close();
        } catch (IOException e) {
            FirebaseCrash.report(e);
        }

        notificationManager.showBtTransferFinished(notificationId);
        return data;
    }

    @Override
    protected void onProgressUpdate(Integer[] values) {
        //Runs on UI thread when publishProgress() is called
        super.onProgressUpdate(values);

        FirebaseCrash.logcat(Log.INFO, this.getClass().getName(), "Inserted ScoutData into DB, row=" + values[0]);
    }

    @Override
    protected void onPostExecute(ScoutData o) {
        super.onPostExecute(o);

        if (o != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            FeedEntry feedEntry = new FeedEntry(FeedEntry.EntryType.SERVER_RECEIVED_MATCH, System.currentTimeMillis());

            if (prefs.getBoolean("bt_send_to_local_storage", true)) {
                //Put the fetched ScoutData in the local database
                ScoutDataWriteTask writeTask = new ScoutDataWriteTask(o, context);
                writeTask.execute();

                feedEntry.addOperation(new EntryOperationWrapper(EntryOperationType.SAVED_TO_LOCAL_STORAGE,
                        EntryOperationStatus.OPERATION_SUCCESSFUL)); //TODO determine this based on callback?
            }

            if (prefs.getBoolean("bt_send_to_bt_server", false)) {
                String address = prefs.getString("bt_bt_server_device", null);

                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

                if (device.getName() == null) { //This should catch both the no device selected error and the bluetooth off error
                    throw new NullPointerException("Error initializing Bluetooth!"); //todo better way to notify?
                }

                ClientConnectionThread connectThread = new ClientConnectionThread(device, o, context, null);
                connectThread.start();

                feedEntry.addOperation(new EntryOperationWrapper(EntryOperationType.SENT_TO_BLUETOOTH_SERVER,
                        EntryOperationStatus.OPERATION_SUCCESSFUL)); //TODO determine this based on callback?
            }

            /*if (prefs.getBoolean("bt_send_to_linked_sheet", false)) {
                SheetsUpdateTask task = new SheetsUpdateTask(context);
                task.execute(o);
            }*/

            FeedDataWriteTask feedDataWriteTask = new FeedDataWriteTask(feedEntry, context);
            feedDataWriteTask.execute();
        } else {
            FirebaseCrash.logcat(Log.ERROR, this.getClass().getName(), "Failed to start FeedDataWriteTask!");
        }
    }

}
