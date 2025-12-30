package com.example.group316weatherappproject;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Basic instrumented test that verifies the application context is correctly
 * initialized on an Android device. This test confirms that the runtime
 * package name matches the expected identifier defined in the project
 * configuration, ensuring the app under test is properly deployed and the
 * testing environment is correctly targeted.
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    /**
     * Validates that the app's context resolves to the correct package name.
     * This confirms the test is interacting with the wanted application build.
     */
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.group316weatherappproject", appContext.getPackageName());
    }
}
