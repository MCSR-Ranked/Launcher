/*
 * MCSR Ranked Launcher - https://github.com/RedLime/MCSR-Ranked-Launcher
 * Copyright (C) 2023 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.data.microsoft.Entitlements;
import com.atlauncher.data.microsoft.LoginResponse;
import com.atlauncher.data.microsoft.OauthTokenResponse;
import com.atlauncher.data.microsoft.Profile;
import com.atlauncher.data.microsoft.XboxLiveAuthErrorResponse;
import com.atlauncher.data.microsoft.XboxLiveAuthResponse;
import com.atlauncher.gui.panels.LoadingPanel;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.DownloadException;
import com.atlauncher.utils.MicrosoftAuthAPI;
import com.atlauncher.utils.OS;
import com.google.gson.JsonObject;

import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.VirtualHost;

public final class LoginWithMicrosoftDialog extends JDialog {
    private static final HTTPServer server = new HTTPServer(Constants.MICROSOFT_LOGIN_REDIRECT_PORT);
    private static final VirtualHost host = server.getVirtualHost(null);

    private final MicrosoftAccount account;

    public LoginWithMicrosoftDialog() {
        this(null);
    }

    public LoginWithMicrosoftDialog(MicrosoftAccount account) {
        super(App.launcher.getParent(), GetText.tr("Login with Microsoft"), ModalityType.DOCUMENT_MODAL);

        final boolean useLocalServer = App.settings.loginByLocalServer;

        this.account = account;
        this.setMinimumSize(new Dimension(400, 400));
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        LoadingPanel loadingPanel = new LoadingPanel(GetText.tr("Loading Microsoft Authentication..."));
        this.add(loadingPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel linkPanel = new JPanel(new FlowLayout());

        JTextField linkTextField = new JTextField(useLocalServer ? Constants.MICROSOFT_LOGIN_URL : "");
        linkTextField.setPreferredSize(new Dimension(useLocalServer ? 300 : 100, 23));
        linkPanel.add(linkTextField, BorderLayout.SOUTH);

        AtomicReference<String> verificationUri = new AtomicReference<>("");
        AtomicReference<String> userCode = new AtomicReference<>("");

        JButton linkCopyButton = new JButton("Copy" + (useLocalServer ? "" : " & Login"));
        if (useLocalServer) {
            linkCopyButton.addActionListener(e -> {
                linkTextField.selectAll();
                OS.copyToClipboard(Constants.MICROSOFT_LOGIN_URL);
            });
        } else {
            linkCopyButton.addActionListener(e -> {
                if (!verificationUri.get().isEmpty() && !userCode.get().isEmpty()) {
                    OS.copyToClipboard(userCode.get());
                    OS.openWebBrowser(verificationUri.get());
                }
            });
            linkCopyButton.setEnabled(false);
        }
        linkPanel.add(linkCopyButton);

        if (useLocalServer) {
            JLabel infoLabel = new JLabel("<html>"
                + GetText.tr("If your browser hasn't opened, please manually open the below link in your browser")
                + "</html>");
            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 32));

            bottomPanel.add(infoLabel, BorderLayout.CENTER);
        }
        bottomPanel.add(linkPanel, BorderLayout.SOUTH);

        this.add(bottomPanel, BorderLayout.SOUTH);

        setVisible(false);
        dispose();

        Thread thread = null;
        try {
            if (useLocalServer) {
                OS.openWebBrowser(Constants.MICROSOFT_LOGIN_URL);
                startServer();
            }
            else {
                thread = new Thread(() -> {
                    try {
                        JsonObject resultCode = MicrosoftAuthAPI.getDeviceAuthCode();
                        loadingPanel.updateText("Click to 'Copy code and Login' button and then paste code.");
                        linkTextField.setText(resultCode.get("user_code").getAsString());
                        userCode.set(linkTextField.getText());
                        verificationUri.set(resultCode.get("verification_uri").getAsString());
                        linkCopyButton.setEnabled(true);

                        int interval = resultCode.get("interval").getAsInt();
                        long expires = System.currentTimeMillis() + resultCode.get("expires_in").getAsInt() * 1000L;
                        while (true) {
                            //noinspection BusyWait
                            Thread.sleep(interval * 1000L);
                            OauthTokenResponse oauthTokenResponse = MicrosoftAuthAPI.getDeviceAuthToken(resultCode.get("device_code").getAsString());
                            if (oauthTokenResponse == null) {
                                if (System.currentTimeMillis() < expires) {
                                    LogManager.warn("Authentication in progress...");
                                } else {
                                    LogManager.error("Failed to Authentication Microsoft Account");

                                    DialogManager.okDialog()
                                        .setType(DialogManager.ERROR)
                                        .setTitle(GetText.tr("Error!"))
                                        .setContent(GetText.tr("Failed to Authentication Microsoft Account"))
                                        .show();
                                    return;
                                }
                            } else {
                                loadingPanel.updateText("Processing Authentication...");
                                linkCopyButton.setEnabled(false);
                                acquireXBLToken(oauthTokenResponse);
                                close();

                                DialogManager.okDialog()
                                    .setTitle(GetText.tr("Done!"))
                                    .setContent(GetText.tr("Done with Microsoft Account Authentication!"))
                                    .show();
                                break;
                            }
                        }
                    } catch (InterruptedException ignored) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            LogManager.logStackTrace("Error starting web server for Microsoft login", e);

            close();
        }

        this.setLocationRelativeTo(App.launcher.getParent());
        this.setVisible(true);

        if (thread != null) {
            thread.interrupt();
        }
    }

    private void close() {
        server.stop();
        setVisible(false);
        dispose();
    }

    private void startServer() throws IOException {
        host.addContext("/", (req, res) -> {
            req.getHeaders().add("Access-Control-Allow-Origin", "*");
            if (req.getParams().containsKey("error")) {
                res.getHeaders().add("Content-Type", "text/plain");
                res.send(500, GetText.tr("Error logging in. Check console for more information"));
                LogManager.error("Error logging into Microsoft account: " + URLDecoder
                        .decode(req.getParams().get("error_description"), StandardCharsets.UTF_8.toString()));
                close();
                return 0;
            }

            if (!req.getParams().containsKey("code")) {
                res.getHeaders().add("Content-Type", "text/plain");
                res.send(400, GetText.tr("Code is missing"));
                close();
                return 0;
            }

            try {
                acquireAccessToken(req.getParams().get("code"));
            } catch (Exception e) {
                LogManager.logStackTrace("Error acquiring accessToken", e);
                res.getHeaders().add("Content-Type", "text/html");
                res.send(500, GetText.tr("Error logging in. Check console for more information"));
                close();
                return 0;
            }

            res.getHeaders().add("Content-Type", "text/plain");
            // #. {0} is the name of the launcher
            res.send(200, GetText.tr("Login complete. You can now close this window and go back to {0}",
                    Constants.LAUNCHER_NAME));
            close();
            return 0;
        }, "GET");

        server.start();
    }

    private void addAccount(OauthTokenResponse oauthTokenResponse, XboxLiveAuthResponse xstsAuthResponse,
            LoginResponse loginResponse, Profile profile) {
        if (account != null || AccountManager.isAccountByName(loginResponse.username)) {
            MicrosoftAccount account = (MicrosoftAccount) AccountManager.getAccountByName(loginResponse.username);

            if (account == null) {
                return;
            }

            // if forced to relogin, then make sure they logged into correct account
            if (this.account != null && !account.username.equals(this.account.username)) {
                DialogManager.okDialog().setTitle(GetText.tr("Incorrect account"))
                        .setContent(
                                GetText.tr("Logged into incorrect account. Please login again on the Accounts tab."))
                        .setType(DialogManager.ERROR).show();
                return;
            }

            account.update(oauthTokenResponse, xstsAuthResponse, loginResponse, profile);
            AccountManager.saveAccounts();
        } else {
            MicrosoftAccount account = new MicrosoftAccount(oauthTokenResponse, xstsAuthResponse, loginResponse,
                    profile);

            AccountManager.addAccount(account);
        }
    }

    private void acquireAccessToken(String authcode) throws Exception {
        OauthTokenResponse oauthTokenResponse = MicrosoftAuthAPI.tradeCodeForAccessToken(authcode);

        acquireXBLToken(oauthTokenResponse);
    }

    private void acquireXBLToken(OauthTokenResponse oauthTokenResponse) throws Exception {
        XboxLiveAuthResponse xblAuthResponse = MicrosoftAuthAPI.getXBLToken(oauthTokenResponse.accessToken);

        acquireXsts(oauthTokenResponse, xblAuthResponse.token);
    }

    private void acquireXsts(OauthTokenResponse oauthTokenResponse, String xblToken) throws Exception {
        XboxLiveAuthResponse xstsAuthResponse = null;

        try {
            xstsAuthResponse = MicrosoftAuthAPI.getXstsToken(xblToken);
        } catch (DownloadException e) {
            if (e.response != null) {
                LogManager.debug(Gsons.DEFAULT.toJson(e.response));
                XboxLiveAuthErrorResponse xboxLiveAuthErrorResponse = Gsons.DEFAULT.fromJson(e.response,
                        XboxLiveAuthErrorResponse.class);

                String error = xboxLiveAuthErrorResponse.getErrorMessageForCode();

                if (error != null) {
                    LogManager.warn(error);
                    DialogManager.okDialog().setTitle(GetText.tr("Error logging into Xbox Live"))
                            .setContent(new HTMLBuilder().center().text(error).build()).setType(DialogManager.ERROR)
                            .show();

                    String link = xboxLiveAuthErrorResponse.getBrowserLinkForCode();

                    if (link != null) {
                        OS.openWebBrowser(link);
                    }
                }

                throw e;
            }
        }

        if (xstsAuthResponse != null) {
            acquireMinecraftToken(oauthTokenResponse, xstsAuthResponse);
        }
    }

    private void acquireMinecraftToken(OauthTokenResponse oauthTokenResponse, XboxLiveAuthResponse xstsAuthResponse)
            throws Exception {
        String xblUhs = xstsAuthResponse.displayClaims.xui.get(0).uhs;
        String xblXsts = xstsAuthResponse.token;

        LoginResponse loginResponse = MicrosoftAuthAPI.loginToMinecraft("XBL3.0 x=" + xblUhs + ";" + xblXsts);

        if (loginResponse == null) {
            throw new Exception("Failed to login to Minecraft");
        }

        Entitlements entitlements = MicrosoftAuthAPI.getEntitlements(loginResponse.accessToken);

        if (!(entitlements.items.stream().anyMatch(i -> i.name.equalsIgnoreCase("product_minecraft"))
                && entitlements.items.stream().anyMatch(i -> i.name.equalsIgnoreCase("game_minecraft")))) {
            DialogManager.okDialog().setTitle(GetText.tr("Minecraft Has Not Been Purchased"))
                    .setContent(new HTMLBuilder().center().text(GetText.tr(
                            "This account doesn't have a valid purchase of Minecraft.<br/><br/>Please make sure you've bought the Java edition of Minecraft and then try again."))
                            .build())
                    .setType(DialogManager.ERROR).show();
            throw new Exception("Account does not own Minecraft");
        }

        Profile profile;
        try {
            profile = MicrosoftAuthAPI.getMcProfile(loginResponse.accessToken);
        } catch (DownloadException e) {
            LogManager.error("Minecraft profile not found");

            new CreateMinecraftProfileDialog(loginResponse.accessToken);

            try {
                profile = MicrosoftAuthAPI.getMcProfile(loginResponse.accessToken);
            } catch (IOException e1) {
                LogManager.logStackTrace("Failed to get Minecraft profile", e1);
                throw new Exception("Failed to get Minecraft profile");
            }
        }

        if (profile == null) {
            throw new Exception("Failed to get Minecraft profile");
        }

        // add the account
        addAccount(oauthTokenResponse, xstsAuthResponse, loginResponse, profile);
    }
}
