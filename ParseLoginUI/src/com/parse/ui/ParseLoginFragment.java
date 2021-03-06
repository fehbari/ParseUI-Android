/*
 *  Copyright (c) 2014, Facebook, Inc. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Facebook.
 *
 *  As with any software that integrates with the Facebook platform, your use of
 *  this software is subject to the Facebook Developer Principles and Policies
 *  [http://developers.facebook.com/policy/]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * Fragment for the user login screen.
 */
public class ParseLoginFragment extends ParseLoginFragmentBase {

    public interface ParseLoginFragmentListener {
        public void onSignUpClicked(String username, String password);

        public void onLoginHelpClicked(String username);
    }

    private static final String LOG_TAG = "ParseLoginFragment";
    private static final String USER_OBJECT_NAME_FIELD = "name";

    private View parseLogin;
    private EditText usernameField;
    private EditText passwordField;
    private TextView parseLoginHelpButton;
    private Button parseLoginButton;
    private Button parseSignupButton;
    private Button facebookLoginButton;
    private ParseLoginFragmentListener loginFragmentListener;
    private ParseOnLoginSuccessListener onLoginSuccessListener;

    private ParseLoginConfig config;

    public static ParseLoginFragment newInstance(Bundle configOptions) {
        ParseLoginFragment loginFragment = new ParseLoginFragment();
        loginFragment.setArguments(configOptions);
        return loginFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

        View v = inflater.inflate(R.layout.com_parse_ui_parse_login_fragment,
                parent, false);
        ParseTextView appLogo = (ParseTextView) v.findViewById(R.id.app_logo);
        parseLogin = v.findViewById(R.id.parse_login);
        usernameField = (EditText) v.findViewById(R.id.login_username_input);
        passwordField = (EditText) v.findViewById(R.id.login_password_input);
        parseLoginHelpButton = (Button) v.findViewById(R.id.parse_login_help);
        parseLoginButton = (Button) v.findViewById(R.id.parse_login_button);
        parseSignupButton = (Button) v.findViewById(R.id.parse_signup_button);
        facebookLoginButton = (Button) v.findViewById(R.id.facebook_login);
        View parseSocialContainer = v.findViewById(R.id.parse_social_buttons);

        setupRippleEffect();

        if (allowParseLoginAndSignup()) {
            setUpParseLoginAndSignup();
        }
        if (allowFacebookLogin()) {
            setUpFacebookLogin();
        }

        if (isChina()) {
            // Hide Facebook login in China.
            parseSocialContainer.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ParseLoginFragmentListener) {
            loginFragmentListener = (ParseLoginFragmentListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseLoginFragmentListener");
        }

        if (activity instanceof ParseOnLoginSuccessListener) {
            onLoginSuccessListener = (ParseOnLoginSuccessListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoginSuccessListener");
        }

        if (activity instanceof ParseOnLoadingListener) {
            onLoadingListener = (ParseOnLoadingListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoadingListener");
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    private void setupRippleEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int background = R.drawable.parse_button_background_ripple;
            int facebookBackground = R.drawable.parse_facebook_button_background_ripple;

            parseLoginButton.setBackgroundResource(background);
            parseSignupButton.setBackgroundResource(background);
            facebookLoginButton.setBackgroundResource(facebookBackground);
        }
    }

    private void setUpParseLoginAndSignup() {
        parseLogin.setVisibility(View.VISIBLE);

        if (config.isParseLoginEmailAsUsername()) {
            usernameField.setHint(R.string.com_parse_ui_email_input_hint);
            usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        if (config.getParseLoginButtonText() != null) {
            parseLoginButton.setText(config.getParseLoginButtonText());
        }

        parseLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        if (config.getParseSignupButtonText() != null) {
            parseSignupButton.setText(config.getParseSignupButtonText());
        }

        parseSignupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString().replaceAll("\\s+", "").toLowerCase();
                String password = passwordField.getText().toString();

                loginFragmentListener.onSignUpClicked(username, password);
            }
        });

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performLogin();
                }
                return true;
            }
        });

        if (config.getParseLoginHelpText() != null) {
            parseLoginHelpButton.setText(config.getParseLoginHelpText());
        }

        parseLoginHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString().replaceAll("\\s+", "").toLowerCase();
                loginFragmentListener.onLoginHelpClicked(username);
            }
        });
    }

    private void performLogin() {
        final String username = usernameField.getText().toString().replaceAll("\\s+", "").toLowerCase();
        usernameField.setText(username);
        String password = passwordField.getText().toString();

        if (username.length() == 0) {
            showToast(R.string.com_parse_ui_no_email_toast);
        } else if (password.length() == 0) {
            showToast(R.string.com_parse_ui_no_password_toast);
        } else {
            loadingStart(true);
            ParseUser.logInInBackground(username, password, new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (isActivityDestroyed()) {
                        return;
                    }

                    if (user != null) {
                        loadingFinish();
                        loginSuccess(username);
                    } else {
                        loadingFinish();
                        if (e != null) {
                            debugLog(getString(R.string.com_parse_ui_login_warning_parse_login_failed) +
                                    e.toString());
                            if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                                if (config.getParseLoginInvalidCredentialsToastText() != null) {
                                    showToast(config.getParseLoginInvalidCredentialsToastText());
                                } else {
                                    showToast(R.string.com_parse_ui_parse_login_invalid_credentials_toast);
                                }
                                passwordField.selectAll();
                                passwordField.requestFocus();
                            } else {
                                showToast(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                            }
                        }
                    }
                }
            });
        }
    }

    private void setUpFacebookLogin() {
        facebookLoginButton.setVisibility(View.VISIBLE);

        if (config.getFacebookLoginButtonText() != null) {
            facebookLoginButton.setText(config.getFacebookLoginButtonText());
        }

        facebookLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Collection<String> permissions = config.getFacebookLoginPermissions();
                if (permissions == null) permissions = new ArrayList<>();
                permissions.add("email");

                ParseFacebookUtils.logInWithReadPermissionsInBackground(getActivity(), permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (isActivityDestroyed()) {
                            return;
                        }

                        if (user == null) {
                            if (e != null) {
                                showToast(R.string.com_parse_ui_facebook_login_failed_toast);
                                debugLog(getString(R.string.com_parse_ui_login_warning_facebook_login_failed) +
                                        e.toString());
                            }
                        } else {
                            loadingStart();

                            GraphRequest request = GraphRequest.newMeRequest(
                                    AccessToken.getCurrentAccessToken(),
                                    new GraphRequest.GraphJSONObjectCallback() {
                                        @Override
                                        public void onCompleted(
                                                JSONObject object,
                                                GraphResponse response) {
                                            /*
                                                If we were able to successfully retrieve the Facebook
                                                email, set it on the "email" and "username" fields.
                                                Also set the gender if we can read it.
                                            */
                                            ParseUser parseUser = ParseUser.getCurrentUser();
                                            if (object != null && parseUser != null) {
                                                try {
                                                    final String email = object.getString("email");
                                                    if (email != null && !email.isEmpty()) {

                                                        parseUser.put("email", email);
                                                        parseUser.put("username", email);

                                                        String gender = object.getString("gender");
                                                        if (gender != null && !gender.isEmpty()) {
                                                            parseUser.put("gender", gender);
                                                        }

                                                        parseUser.saveInBackground(new SaveCallback() {
                                                            @Override
                                                            public void done(ParseException e) {
                                                                if (e != null) {
                                                                    debugLog(getString(
                                                                            R.string.com_parse_ui_login_warning_facebook_login_user_update_failed) +
                                                                            e.toString());
                                                                }
                                                                loadingFinish();
                                                                loginSuccess(email);
                                                            }
                                                        });
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(LOG_TAG, "Failed to retrieve Facebook user properties.", e);
                                                    loadingFinish();
                                                }
                                            }
                                        }
                                    });

                            Bundle parameters = new Bundle();
                            parameters.putString("fields", "email,gender");
                            request.setParameters(parameters);
                            request.executeAsync();
                        }
                    }
                });
            }
        });
    }

    private boolean allowParseLoginAndSignup() {
        if (!config.isParseLoginEnabled()) {
            return false;
        }

        if (usernameField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_username_field);
        }
        if (passwordField == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_password_field);
        }
        if (parseLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_button);
        }
        if (parseSignupButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_signup_button);
        }
        if (parseLoginHelpButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_help_button);
        }

        boolean result = (usernameField != null) && (passwordField != null)
                && (parseLoginButton != null) && (parseSignupButton != null)
                && (parseLoginHelpButton != null);

        if (!result) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_username_password_login);
        }
        return result;
    }

    private boolean allowFacebookLogin() {
        if (!config.isFacebookLoginEnabled()) {
            return false;
        }

        if (isChina()) return false;

        if (facebookLoginButton == null) {
            debugLog(R.string.com_parse_ui_login_warning_disabled_facebook_login);
            return false;
        } else {
            return true;
        }
    }

    private void loginSuccess(String email) {
        onLoginSuccessListener.onLoginSuccess(email);
    }

    private boolean isChina() {
        boolean isChina = false;

        // Try to check country with the SIM card.
        try {
            TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            String countryIso = manager.getSimCountryIso();

            // Check country code of SIM card.
            if (countryIso != null) {
                isChina = countryIso.equals("CN") || countryIso.equals("CHN");
            }
        } catch (Exception e) {
            Log.e("ParseLoginFragment", "Error retrieving country code with SIM card.", e);
        }

        // Fallback to locale check if needed.
        if (!isChina) {
            isChina = Locale.getDefault().getCountry().equals("CN");
        }

        return isChina;
    }

}
