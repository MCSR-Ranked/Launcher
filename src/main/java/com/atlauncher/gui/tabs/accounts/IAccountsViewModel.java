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
package com.atlauncher.gui.tabs.accounts;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlauncher.data.AbstractAccount;
import com.atlauncher.data.MicrosoftAccount;

/**
 * 12 / 06 / 2022
 * The view model for Accounts tab, handles all background activity
 */
public interface IAccountsViewModel {

    /**
     * Request that the UI be populated with new accounts
     */
    void pushNewAccounts();

    /**
     * Called when an account has been selected, update UI accordingly
     *
     * @param onAccountSelected function to call
     */
    void onAccountSelected(Consumer<AbstractAccount> onAccountSelected);

    /**
     * Get the count of accounts in the launcher
     * @return count of accounts
     */
    int accountCount();

    /**
     * Called when accounts list should be updated
     *
     * @param onAccountsChanged function to call
     */
    void onAccountsNamesChanged(Consumer<List<String>> onAccountsChanged);

    /**
     * Set the currently selected account, will cause [onAccountSelected] to
     * be called.
     *
     * @param index index of the account, as provided by the combo box.
     */
    void setSelectedAccount(int index);

    /**
     * Get the selected index
     *
     * @return UI index of selected item
     */
    int getSelectedIndex();

    /**
     * Update the username of a legacy account
     */
    void updateUsername();

    /**
     * Change the skin of an account
     */
    void changeSkin();

    /**
     * Update the skin of an account
     */
    void updateSkin();

    /**
     * Delete the account from the launcher
     */
    void deleteAccount();

    /**
     * Refresh the access token for an account
     *
     * @return if refresh was successful
     */
    boolean refreshAccessToken();

    // Account login

    /**
     * Get currently selected account
     *
     * @return the currently selected account
     */
    @NotNull
    AbstractAccount getSelectedAccount();

    /**
     * Get the selected account as a Microsoft account
     *
     * @return the currently selected microsoft account, otherwise null
     */
    @Nullable
    MicrosoftAccount getSelectedAccountAs();

    /**
     * Pre-check result for login
     */
    abstract class LoginPreCheckResult {
        /**
         * The account already exists
         */
        static class Exists extends LoginPreCheckResult {
        }
    }

    /**
     * Post result for login
     */
    abstract class LoginPostResult {
        /**
         * The account was added
         */
        static class Added extends LoginPostResult {
        }

        /**
         * The account was edited
         */
        static class Edited extends LoginPostResult {
        }

        /**
         * An error occured adding the account
         */
        static class Error extends LoginPostResult {
            @Nullable
            String errorContent;

            public Error(@Nullable String errorContent) {
                this.errorContent = errorContent;
            }
        }
    }
}
