package org.bamboomy.delete.deletespotifytrack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.bamboomy.delete.deletespotifytrack.DeleteActivity.CHOOSE_KEY;
import static org.bamboomy.delete.deletespotifytrack.MainActivity.CLIENT_ID_ONE;
import static org.bamboomy.delete.deletespotifytrack.MainActivity.NOTIFICATION_ID_2;
import static org.bamboomy.delete.deletespotifytrack.MainActivity.SKIP_TO_NEXT_GOLD;

public class HarvestActivity extends AppCompatActivity {

    public static final String CLIENT_ID;
    public static final int AUTH_TOKEN_REQUEST_CODE = 0x10;
    public static final int AUTH_TOKEN_HARVEST = 0x11;

    private String listId = "", harvestListName;

    private boolean aboutToDelete = false, aboutToAdd = false;

    public static final String DEFAULT_LIST_NAME = "Sort Spotify track";

    static {

        /*
        if (Math.random() < 0.5) {


        } else {

            CLIENT_ID = CLIENT_ID_TWO;
        }
        */

        CLIENT_ID = CLIENT_ID_ONE;
    }


    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken;
    private Call mCall;

    private JSONObject json;
    //private String playlist;
    private String track, artistString, playlistString;
    private String uri = "1", oldUri = "2";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private NotificationManager nMN;

    public static final String CHOOSE_KEY = "choose_key";

    private boolean editFlag = false, addFlag = false;

    private CharSequence jsonUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        checkOnline();

        final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
        AuthenticationClient.openLoginActivity(HarvestActivity.this, AUTH_TOKEN_HARVEST, request);

        showNotification();

        Log.d("delete", "oncreate called");

    }

    private void showNotification() {

        Intent myIntent = new Intent(this, HarvestActivity.class);

        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        NotificationCompat.Builder notificationCompatBuilder;

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        2,
                        myIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String CHANNEL_ID = "my_channel_02";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            /* Create or update. */
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            nMN.createNotificationChannel(channel);
        }

        notificationCompatBuilder =
                new NotificationCompat.Builder(
                        getApplicationContext(), CHANNEL_ID);

        Notification n = notificationCompatBuilder
                .setContentTitle("Tap to harvest current song.")
                .setSmallIcon(R.drawable.chest)
                .setContentIntent(notifyPendingIntent)
                .build();

        nMN.notify(NOTIFICATION_ID_2, n);
    }

    private AuthenticationRequest getAuthenticationRequest(AuthenticationResponse.Type type) {
        return new AuthenticationRequest.Builder(CLIENT_ID, type, getRedirectUri().toString())
                .setShowDialog(false)
                .setScopes(new String[]{"user-read-playback-state", "playlist-modify-private", "playlist-modify-public",
                        "user-modify-playback-state"})
                .build();
    }

    private Uri getRedirectUri() {
        return new Uri.Builder()
                .scheme("deleteapp")
                .authority("ismydeleteapp")
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);

        if (AUTH_TOKEN_HARVEST == requestCode) {

            mAccessToken = response.getAccessToken();

            addFlag = true;
            editFlag = false;

            getId();
        }
    }

    private void getId() {

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch id data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                Log.d("delete", response.toString());

                try {

                    showLists(new JSONObject(response.body().string()).getString("id"));

                } catch (JSONException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(HarvestActivity.this, "something wrong sith json: ",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    private void showLists(final String id) {

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=50")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch list data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                Log.d("lists", response.toString());

                try {

                    JSONArray array = new JSONObject(response.body().string()).getJSONArray("items");

                    final ArrayList<List> result = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {

                        JSONObject jsonList = array.getJSONObject(i);

                        if (jsonList.getJSONObject("owner").getString("id").equalsIgnoreCase(id)) {

                            List list = new List();

                            list.setName(jsonList.getString("name"));

                            list.setId(jsonList.getString("id"));

                            result.add(list);
                        }
                    }

                    SharedPreferences sharedPrefs = PreferenceManager
                            .getDefaultSharedPreferences(HarvestActivity.this);

                    if (sharedPrefs.getString(CHOOSE_KEY, CHOOSE_KEY).equalsIgnoreCase(CHOOSE_KEY)) {

                        createDefaultList(id);

                    } else {

                        if (addFlag) {

                            addFlag = false;

                            for (List list : result) {

                                if (list.getName().equalsIgnoreCase(sharedPrefs.getString(CHOOSE_KEY, CHOOSE_KEY))) {

                                    listId = list.getId();

                                    harvestListName = list.getName();

                                    add();

                                    break;
                                }
                            }

                        } else {

                            throw new RuntimeException("addflag should be set here");
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(HarvestActivity.this, "something wrong sith json2: ",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

    }

    private void createDefaultList(final String id) {

        JSONObject object = new JSONObject();

        try {

            object.put("name", DEFAULT_LIST_NAME);

            RequestBody body = RequestBody.create(JSON, object.toString());

            final Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/users/" + id + "/playlists")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + mAccessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            cancelCall();
            mCall = mOkHttpClient.newCall(request);

            mCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(HarvestActivity.this, "Failed to fetch list data: " + e,
                                    Toast.LENGTH_LONG).show();

                        }
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            SharedPreferences sharedPrefs = PreferenceManager
                                    .getDefaultSharedPreferences(HarvestActivity.this);

                            final SharedPreferences.Editor editor = sharedPrefs.edit();

                            editor.putString(CHOOSE_KEY, DEFAULT_LIST_NAME);

                            editor.commit();

                            MainActivity.defaultList = true;

                            showLists(id);
                        }
                    });

                }
            });


        } catch (final JSONException e) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(HarvestActivity.this, "Json exception... : " + e,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void add() {

        aboutToAdd = true;

        refresh();
    }

    private void checkOnline() {

        final ConnectivityManager connMgr = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        Log.d("dd", wifi.isConnectedOrConnecting() + ", " + mobile.isConnectedOrConnecting());

        if (!wifi.isConnectedOrConnecting() && !mobile.isConnectedOrConnecting()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("You're not online...");

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });

            builder.show();
        }
    }

    private void refresh() {

        if (mAccessToken == null) {
            return;
        }

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me/player")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch data: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());

                    if (!jsonObject.getBoolean("is_playing")) {

                        if ((aboutToDelete || aboutToAdd)) {

                            NotPlayingDialog lockedDialog = new NotPlayingDialog();

                            lockedDialog.setAction("add");

                            lockedDialog.show(HarvestActivity.this.getSupportFragmentManager(), "test");

                        } else {

                            Toast.makeText(HarvestActivity.this, "No song is currently playing... ",
                                    Toast.LENGTH_LONG).show();
                        }

                        return;
                    }

                    /*
                    final Request request = new Request.Builder()
                            .url("https://api.spotify.com/v1/playlists/" + splitted[4])
                            .addHeader("Authorization", "Bearer " + mAccessToken)
                            .build();

                    playlist = splitted[4];

                    final String name = jsonObject.getJSONObject("item").getString("name");

                    track = name;

                    final String artist = jsonObject.getJSONObject("item").getJSONArray("artists").getJSONObject(0)
                            .getString("name");

                    artistString = artist;

                    oldUri = uri;
                    uri = jsonObject.getJSONObject("item").getString("uri");

                    jsonUri = jsonObject.getJSONObject("item").getString("uri")
                            .replace(":", "%3A");

                    cancelCall();
                    mCall = mOkHttpClient.newCall(request);

                    mCall.enqueue(getNameCallback());
                    */

                    jsonUri = jsonObject.getJSONObject("item").getString("uri")
                            .replace(":", "%3A");

                    aboutToAdd = false;

                    performAdd();

                } catch (final JSONException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (e.getMessage().contains("at character 0")) {

                                NotPlayingDialog lockedDialog = new NotPlayingDialog();

                                lockedDialog.show(HarvestActivity.this.getSupportFragmentManager(), "test");

                            } else {

                                Toast.makeText(HarvestActivity.this, "Failed to parse data: " + e,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private Callback getNameCallback() {

        return new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch name data: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {

                    final String output = new JSONObject(response.body().string()).getString("name");

                    playlistString = output;

                    aboutToAdd = false;

                    performAdd();

                } catch (final JSONException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(HarvestActivity.this, "Failed to parse name data: " + e,
                                    Toast.LENGTH_LONG).show();

                        }
                    });
                }
            }
        };
    }

    private void performAdd() {

        RequestBody body = RequestBody.create(JSON, "");

        Log.d("add", jsonUri + " -> " + listId);

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + listId + "/tracks?uris=" + jsonUri)
                .post(body)
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch delete data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                Log.d("add", response.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, " " + track
                                        + "\n added to playlist: \n " + harvestListName,
                                Toast.LENGTH_LONG).show();
                    }
                });

                final SharedPreferences sharedPrefs = PreferenceManager
                        .getDefaultSharedPreferences(HarvestActivity.this);

                if (sharedPrefs.getBoolean(SKIP_TO_NEXT_GOLD, true)) {

                    RequestBody body = RequestBody.create(JSON, "");

                    final Request request = new Request.Builder()
                            .url("https://api.spotify.com/v1/me/player/next")
                            .addHeader("Authorization", "Bearer " + mAccessToken)
                            .post(body)
                            .build();

                    cancelCall();
                    mCall = mOkHttpClient.newCall(request);

                    mCall.enqueue(getHarvestedCallback());

                } else {

                    Intent i = new Intent(HarvestActivity.this, MainActivity.class);

                    i.putExtra("key", 1);

                    startActivity(i);

                    finish();
                }
            }
        });

    }

    private Callback getHarvestedCallback() {

        return new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Failed to fetch skip data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(HarvestActivity.this, "Harvested...",
                                Toast.LENGTH_LONG).show();

                        Intent i = new Intent(HarvestActivity.this, MainActivity.class);

                        i.putExtra("key", 1);

                        startActivity(i);

                        finish();
                    }
                });
            }
        };
    }

}
