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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static org.bamboomy.delete.deletespotifytrack.DeleteActivity.CHOOSE_KEY;

public class ChooseListDialog extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private AppCompatActivity activity;

    private CheckBox neverAgain;

    private SharedPreferences sharedPrefs;
    private ArrayList<List> list;
    private String confirm = "";

    public static final String DONTASK_LISTS = "DONTASK_LISTS";

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
        View v = inflater.inflate(R.layout.choose_list, container, false);

        v.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updatePref();

                dismiss();
            }
        });

        if (!confirm.equalsIgnoreCase("")) {

            ((TextView) v.findViewById(R.id.confirm)).setText(confirm);
        }

        neverAgain = v.findViewById(R.id.neverAgain);

        String[] items = new String[list.size()];

        for (int i = 0; i < list.size(); i++) {

            items[i] = list.get(i).getName();
        }

        //get the spinner from the xml.
        Spinner dropdown = v.findViewById(R.id.list_of_lists);
        //create a list of items for the spinner.

        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_single_choice, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        dropdown.setOnItemSelectedListener(this);

        return v;
    }

    private void updatePref() {

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(activity);

        final SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putBoolean(DONTASK_LISTS, neverAgain.isChecked());

        editor.commit();
    }

    public void setData(AppCompatActivity mainActivity, SharedPreferences sharedPrefs, ArrayList<List> result, String confirm) {

        activity = mainActivity;
        this.sharedPrefs = sharedPrefs;
        list = result;
        this.confirm = confirm;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(activity);

        final SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString(CHOOSE_KEY, list.get(position).getName());

        editor.commit();

        ((TextView) activity.findViewById(R.id.goldList)).setText(list.get(position).getName());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

        throw new RuntimeException("hope this doesn't happen");
    }
}