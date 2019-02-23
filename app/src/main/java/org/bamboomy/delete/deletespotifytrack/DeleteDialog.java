/*
 * Copyright (c) 2016 Sander Theetaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bamboomy.delete.deletespotifytrack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class DeleteDialog extends DialogFragment {

    private CapableToDeleteActivity activity;

    private String playlistString, artistString, track;

    private CheckBox neverAgain;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Create the AlertDialog object and return it
        return builder.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.delete_ack, container, false);

        TextView text = v.findViewById(R.id.confirm);

        text.setText("This will delete the track: \n " + track + "\n by: " + artistString + "\n from playlist: \n " + playlistString + "\n Are you sure?");

        Button yes = v.findViewById(R.id.yes);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                activity.performDelete();

                updatePref();

                dismiss();
            }
        });

        Button no = v.findViewById(R.id.no);

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updatePref();

                dismiss();
            }
        });

        neverAgain = v.findViewById(R.id.neverAgain);

        return v;
    }

    private void updatePref() {

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(activity);

        final SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putBoolean("dontAsk", neverAgain.isChecked());

        editor.commit();
    }

    public void setData(CapableToDeleteActivity mainActivity, String playlistString, String artistString, String track) {

        activity = mainActivity;

        this.artistString = artistString;
        this.playlistString = playlistString;
        this.track = track;
    }
}