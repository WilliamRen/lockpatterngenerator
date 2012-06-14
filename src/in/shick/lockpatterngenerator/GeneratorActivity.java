/*
Copyright 2010-2012 Michael Shick

This file is part of 'Lock Pattern Generator'.

'Lock Pattern Generator' is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at your option)
any later version.

'Lock Pattern Generator' is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
'Lock Pattern Generator'.  If not, see <http://www.gnu.org/licenses/>.
*/
package in.shick.lockpatterngenerator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class GeneratorActivity extends BaseActivity
{
    public static final int DIALOG_SEPARATION_WARNING = 0,
           DIALOG_EXITED_HARD = 1;

    protected LockPatternView mPatternView;
    protected Button mGenerateButton;
    protected Button mSecuritySettingsButton;
    protected ToggleButton mPracticeToggle;
    protected PatternGenerator mGenerator;
    protected int mGridLength;
    protected int mPatternMin;
    protected int mPatternMax;
    protected String mHighlightMode;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // non-UI setup
        mGenerator = new PatternGenerator();
        if(mPreferences.getBoolean("exited_hard", Defaults.EXITED_HARD))
        {
            mPreferences.edit().putBoolean("exited_hard", false).commit();
            showDialog(DIALOG_EXITED_HARD);
        }
        // set a default exception handler to catch out of memory errors
        // gracefully
        final Thread.UncaughtExceptionHandler exceptionHandler =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                if(throwable instanceof OutOfMemoryError) {
                    EmergencyExit.clearAndBail(GeneratorActivity.this);
                }
                // punt if it's not an exception we can handle
                exceptionHandler.uncaughtException(thread, throwable);
            }
        });

        // find views
        setContentView(R.layout.generator_activity);
        mPatternView = (LockPatternView) findViewById(R.id.pattern_view);
        mGenerateButton = (Button) findViewById(R.id.generate_button);
        mSecuritySettingsButton =
            (Button) findViewById(R.id.security_settings_button);
        mPracticeToggle = (ToggleButton) findViewById(R.id.practice_toggle);

        // set up views
        mGenerateButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPatternView.setPattern(mGenerator.getPattern());
                mPatternView.invalidate();
            }
        });

        mSecuritySettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                // warn the poorly-informed and those without a command of
                // English that LPG is separate from the lock
                if(mPreferences.getBoolean("remind_of_separation",
                        Defaults.REMIND_OF_SEPARATION)) {
                    showDialog(DIALOG_SEPARATION_WARNING);
                }
                else {
                    jumpToSecurity();
                }
            }
        });

        mPracticeToggle.setOnCheckedChangeListener(
                new ToggleButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked)
            {
                mGenerateButton.setEnabled(!isChecked);
                mPatternView.setPracticeMode(isChecked);
                mPatternView.invalidate();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        updateFromPrefs();
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog;
        AlertDialog.Builder builder;
        switch(id)
        {
        case DIALOG_SEPARATION_WARNING:
            //set up the users ability to disable this reminder
            View disableView = getLayoutInflater()
                .inflate(R.layout.separation_reminder_disable, null);
            ((CheckBox) disableView.findViewById(R.id.disable_checkbox))
                .setOnCheckedChangeListener(
                        new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton view,
                            boolean checked) {
                        mPreferences.edit().putBoolean("remind_of_separation",
                            checked).commit();
                    }
                }
            );

            builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.notice))
                   .setMessage(getString(R.string.separation_warning))
                   .setIcon(android.R.drawable.ic_dialog_info)
                   .setView(disableView)
                   .setCancelable(true)
                   .setPositiveButton(getString(R.string.cont),
                           new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int id) {
                           jumpToSecurity();
                       }
                   });
            dialog = builder.create();
            
            break;
        case DIALOG_EXITED_HARD:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.notice))
                   .setMessage(getString(R.string.emergency_exit))
                   .setIcon(android.R.drawable.ic_dialog_info)
                   .setCancelable(true)
                   .setPositiveButton(getString(R.string.cont),
                           new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int id) {
                           dialog.dismiss();
                       }
                   });
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.generator_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        switch (item.getItemId())
        {
            case R.id.menu_settings:
                startActivity(new Intent().setClass(this,
                            PreferencesActivity.class));
                return true;
            case R.id.menu_help:
                intent = new Intent().setClass(this, TextWallActivity.class);
                intent.putExtra(TextWallActivity.EXTRA_HTML_ASSET, "help.html");
                startActivity(intent);
                return true;
            case R.id.menu_about:
                intent = new Intent().setClass(this, TextWallActivity.class);
                intent.putExtra(TextWallActivity.EXTRA_HTML_ASSET,
                        "about.html");
                intent.putExtra(TextWallActivity.EXTRA_LAYOUT_RESOURCE,
                        R.layout.text_wall_about);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void jumpToSecurity()
    {
        try
        {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
        catch(android.content.ActivityNotFoundException e)
        {
            Toast.makeText(GeneratorActivity.this,
                    getString(R.string.settings_shortcut_failure),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateFromPrefs()
    {
        int gridLength =
            mPreferences.getInt("grid_length", Defaults.GRID_LENGTH);
        int patternMin =
            mPreferences.getInt("pattern_min", Defaults.PATTERN_MIN);
        int patternMax =
            mPreferences.getInt("pattern_max", Defaults.PATTERN_MAX);
        String highlightMode =
            mPreferences.getString("highlight_mode", Defaults.HIGHLIGHT_MODE);

        // sanity checking
        if(gridLength < 1)
        {
            gridLength = 1;
        }
        if(patternMin < 1)
        {
            patternMin = 1;
        }
        if(patternMax < 1)
        {
            patternMax = 1;
        }
        int nodeCount = (int) Math.pow(gridLength, 2);
        if(patternMin > nodeCount)
        {
            patternMin = nodeCount;
        }
        if(patternMax > nodeCount)
        {
            patternMax = nodeCount;
        }
        if(patternMin > patternMax)
        {
            patternMin = patternMax;
        }

        // only update values that differ
        if(gridLength != mGridLength)
        {
            setGridLength(gridLength);
        }
        if(patternMax != mPatternMax)
        {
            setPatternMax(patternMax);
        }
        if(patternMin != mPatternMin)
        {
            setPatternMin(patternMin);
        }
        if(!highlightMode.equals(mHighlightMode))
        {
            setHighlightMode(highlightMode);
        }
    }

    private void setGridLength(int length)
    {
        mGenerator.setGridLength(length);
        mPatternView.setGridLength(length);
    }
    private void setPatternMin(int nodes)
    {
        mPatternMin = nodes;
        mGenerator.setMinNodes(nodes);
    }
    private void setPatternMax(int nodes)
    {
        mPatternMax = nodes;
        mGenerator.setMaxNodes(nodes);
    }
    private void setHighlightMode(String mode)
    {
        if("no".equals(mode))
        {
            mPatternView.setHighlightMode(new LockPatternView.NoHighlight());
        }
        else if("first".equals(mode))
        {
            mPatternView.setHighlightMode(new LockPatternView.FirstHighlight());
        }
        else if("rainbow".equals(mode))
        {
            mPatternView.setHighlightMode(
                    new LockPatternView.RainbowHighlight());
        }

        mHighlightMode = mode;
    }
}
