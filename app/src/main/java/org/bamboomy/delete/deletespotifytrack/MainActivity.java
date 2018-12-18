package org.bamboomy.delete.deletespotifytrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends FragmentActivity {

    public static final String CLIENT_ID = "a8e0c5282c4a4a4eaa47c2172a41507b";
    public static final int AUTH_TOKEN_REQUEST_CODE = 0x10;
    public static final int AUTH_TOKEN_DELETE = 0x11;

    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken;
    private Call mCall;

    private JSONObject json;
    private String playlist, track, artistString, playlistString;
    private String uri = "1", oldUri = "2";

    private boolean refreshed = false;

    private boolean aboutToDelete = false;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View play = findViewById(R.id.delete);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkOnline();

                final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
                AuthenticationClient.openLoginActivity(MainActivity.this, AUTH_TOKEN_DELETE, request);
            }
        });
    }

    private void delete() {

        aboutToDelete = true;

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkOnline();

        if (!refreshed) {
            final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
            AuthenticationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, request);
            refreshed = true;
        }
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

        if (AUTH_TOKEN_REQUEST_CODE == requestCode) {

            mAccessToken = response.getAccessToken();

            refresh();

        } else if (AUTH_TOKEN_DELETE == requestCode) {

            mAccessToken = response.getAccessToken();

            delete();
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

                        Toast.makeText(MainActivity.this, "Failed to fetch data: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());

                    String[] splitted = jsonObject.getJSONObject("context").getString("uri").split(":");

                    if (aboutToDelete && !jsonObject.getBoolean("is_playing")) {

                        NotPlayingDialog lockedDialog = new NotPlayingDialog();

                        lockedDialog.show(MainActivity.this.getSupportFragmentManager(), "test");

                        return;
                    }

                    final Request request = new Request.Builder()
                            .url("https://api.spotify.com/v1/playlists/" + splitted[4])
                            .addHeader("Authorization", "Bearer " + mAccessToken)
                            .build();

                    playlist = splitted[4];

                    final String name = jsonObject.getJSONObject("item").getString("name");

                    track = name;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final TextView codeView = findViewById(R.id.track);
                            codeView.setText("Track: " + name);
                        }
                    });

                    final String artist = jsonObject.getJSONObject("item").getJSONArray("artists").getJSONObject(0)
                            .getString("name");

                    artistString = artist;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final TextView codeView = findViewById(R.id.artist);
                            codeView.setText("Artist: " + artist);
                        }
                    });

                    JSONObject track = new JSONObject();
                    track.put("uri", jsonObject.getJSONObject("item").getString("uri"));

                    oldUri = uri;
                    uri = jsonObject.getJSONObject("item").getString("uri");

                    JSONObject[] array = new JSONObject[]{track};

                    JSONArray tracks = new JSONArray(array);
                    json = new JSONObject();
                    json.put("tracks", tracks);

                    cancelCall();
                    mCall = mOkHttpClient.newCall(request);

                    mCall.enqueue(getNameCallback());

                } catch (final JSONException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(MainActivity.this, "Failed to parse data: " + e,
                                    Toast.LENGTH_LONG).show();
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

                        Toast.makeText(MainActivity.this, "Failed to fetch name data: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {

                    final String output = new JSONObject(response.body().string()).getString("name");

                    playlistString = output;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            final TextView codeView = findViewById(R.id.list);
                            codeView.setText("List: " + output);

                            refreshed = true;

                            if (aboutToDelete) {

                                aboutToDelete = false;

                                showDeleteDialog();
                            }
                        }
                    });

                } catch (final JSONException e) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(MainActivity.this, "Failed to parse name data: " + e,
                                    Toast.LENGTH_LONG).show();

                        }
                    });
                }
            }
        };
    }

    private void showDeleteDialog() {

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (sharedPrefs.getBoolean("dontAsk", false)) {

            if (!oldUri.equalsIgnoreCase(uri)) {

                if (sharedPrefs.getBoolean("dontAck", false)) {

                    performDelete();

                } else {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            AckDialog appstoreDialog = new AckDialog();
                            appstoreDialog.setData(MainActivity.this);
                            appstoreDialog.show(getSupportFragmentManager(), "doesn't matter");
                        }
                    });
                }

            } else {

                performDelete();
            }

        } else {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    DeleteDialog appstoreDialog = new DeleteDialog();
                    appstoreDialog.setData(MainActivity.this, playlistString, artistString, track);
                    appstoreDialog.show(getSupportFragmentManager(), "doesn't matter");
                }
            });
        }
    }

    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    public void onRefresh(View view) {

        checkOnline();

        aboutToDelete = false;

        final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
        AuthenticationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, request);
    }

    public void performDelete() {

        RequestBody body = RequestBody.create(JSON, json.toString());

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + playlist + "/tracks")
                .delete(body)
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

                        Toast.makeText(MainActivity.this, "Failed to fetch delete data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d("delete", response.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(MainActivity.this, " " + track + "\n deleted  from playlist: \n " + playlistString,
                                Toast.LENGTH_LONG).show();
                    }
                });

                RequestBody body = RequestBody.create(JSON, "");

                final Request request = new Request.Builder()
                        .url("https://api.spotify.com/v1/me/player/next")
                        .addHeader("Authorization", "Bearer " + mAccessToken)
                        .post(body)
                        .build();

                cancelCall();
                mCall = mOkHttpClient.newCall(request);

                mCall.enqueue(getDeletedCallback());

            }
        });
    }

    private Callback getDeletedCallback() {

        return new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(MainActivity.this, "Failed to fetch skip data: " + e,
                                Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {

                final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
                AuthenticationClient.openLoginActivity(MainActivity.this, AUTH_TOKEN_REQUEST_CODE, request);
            }
        };
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
}
