package com.example.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages multi-user profiles and emergency contact configuration.
 */
public class UserManager {
    private static final String PREF_NAME = "motionshield_users";
    private static final String KEY_ACTIVE_USER_ID = "active_user_id";
    public static final String HELPLINE_NUMBER = "9440085239";

    public static class UserProfile {
        public final String id;
        public final String name;
        public final String role;
        public final int age;
        public final String emergencyContact;

        public UserProfile(String id, String name, String role, int age, String emergencyContact) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.age = age;
            this.emergencyContact = emergencyContact;
        }
    }

    private static final List<UserProfile> USERS = new ArrayList<>();

    static {
        USERS.add(new UserProfile("user_jahnavi", "Jahnavi", "Primary Account • Fitness & Health", 21, HELPLINE_NUMBER));
        USERS.add(new UserProfile("user_senior", "Senior Care (Elderly)", "Parent Profile • High Fall Risk Mode", 72, HELPLINE_NUMBER));
        USERS.add(new UserProfile("user_guest", "Guest User", "Visitor Profile • Standard Tracking", 28, HELPLINE_NUMBER));
    }

    public static List<UserProfile> getAllUsers() {
        return new ArrayList<>(USERS);
    }

    public static UserProfile getActiveUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String activeId = prefs.getString(KEY_ACTIVE_USER_ID, "user_jahnavi");
        for (UserProfile u : USERS) {
            if (u.id.equals(activeId)) {
                return u;
            }
        }
        return USERS.get(0); // Default to Jahnavi
    }

    public static void setActiveUser(Context context, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply();
    }
}
