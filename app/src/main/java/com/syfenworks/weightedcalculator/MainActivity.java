package com.syfenworks.weightedcalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final ArrayList<View> capsuleList = new ArrayList<>();
    private Toast error_toast;
    private boolean onCreateCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Set true to signal onResume to load the cached save
        //Putting the code in onResume into onCreate caused issues
        onCreateCalled = true;

        //When the layout is touched, remove focus and keyboard
        final RelativeLayout no_focus = findViewById(R.id.no_focus);
        no_focus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        //Calculate button listener
        Button btn_Calculate = findViewById(R.id.btnCalculate);
        btn_Calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calculate();
            }
        });

        //Make the global toast
        error_toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
    }

    ///////////////////////////////////////////////////////////////////

    private void setDefaultCapsules() {
        //Default two capsules
        AddCapsule();
        AddCapsule();
        //Give focus to the first capsule label
        LinearLayout linearLayout = findViewById(R.id.capsules);
        linearLayout.getChildAt(0).requestFocus();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void onPause() {
        super.onPause();

        //Make a temporary save file
        try {
            //Get the cache directory and cached save file
            File temp_save = new File(getFilesDir(), "temp_save.txt");

            //If the file does not exist, create it
            if (!temp_save.exists()) {
                if (!temp_save.createNewFile()) {
                    Log.d("Cached save error", "Could not create new file");
                }
            }
            //If it does exist, delete it and create it again to wipe the data
            else {
                if (!temp_save.delete()) {
                    Log.d("Cached save error", "Could not delete file");
                }
                if (!temp_save.createNewFile()) {
                    Log.d("Cached save error", "Could not create new file");
                }
            }

            //Create the appending file writer
            BufferedWriter writer = new BufferedWriter(new FileWriter(temp_save, true /*appends*/));
            //Write the number of capsules
            writer.write(capsuleList.size() + "\n");

            //Find the current focus
            View current_focus = getCurrentFocus();
            if (current_focus != null) {
                //Obtain the focus' ID
                int focus_Id = current_focus.getId();

                //If the focus is on a capsule editText, the capsule number must be found
                if (focus_Id == R.id.label || focus_Id == R.id.weight || focus_Id == R.id.mark || focus_Id == R.id.total) {
                    //Get the capsule view, find the number
                    View focus_parent_view = (View) current_focus.getParent().getParent().getParent();
                    TextView capsule_number_view = focus_parent_view.findViewById(R.id.capsule_number);
                    String capsule_number = capsule_number_view.getText().toString();

                    //Check which type of capsule editText the focus was on and record a focus code
                    if (focus_Id == R.id.weight) {
                        writer.write(capsule_number + " w\n");
                    }
                    else if (focus_Id == R.id.mark) {
                        writer.write(capsule_number + " m\n");
                    }
                    else if (focus_Id == R.id.total) {
                        writer.write(capsule_number + " t\n");
                    }
                    else {
                        writer.write(capsule_number + " l\n");
                    }
                }
                //Otherwise, the focus is the Final editText
                else {
                    writer.write("0\n");
                }
            }
            //If null, record the focus as the Final editText
            else {
                writer.write("0\n");
            }

            //Record all capsule information
            LinearLayout linearLayout = findViewById(R.id.capsules);
            for (int i = 0; i < capsuleList.size(); i++) {
                //Find the capsule at i
                LinearLayout temp_capsule = (LinearLayout) linearLayout.getChildAt(i);
                //Record label
                EditText temp_label = temp_capsule.findViewById(R.id.label);
                writer.write(temp_label.getText().toString() + "\n");
                //Record weighting
                EditText temp_weighting = temp_capsule.findViewById(R.id.weight);
                writer.write(temp_weighting.getText().toString() + "\n");
                //Record mark
                EditText temp_mark = temp_capsule.findViewById(R.id.mark);
                writer.write(temp_mark.getText().toString() + "\n");
                //Record total
                EditText temp_total = temp_capsule.findViewById(R.id.total);
                writer.write(temp_total.getText().toString() + "\n");
            }

            //Record final
            EditText temp_final = findViewById(R.id.final_mark);
            writer.write(temp_final.getText().toString() + "\n");

            //Close the writer
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Only load the cached save if onCreate was called
        if (onCreateCalled) {
            //Reset the boolean to false
            onCreateCalled = false;
            //If there is no bundle or cached save, set defaults
            File temp_save = new File(getFilesDir(), "temp_save.txt");
            if (!temp_save.exists()) {
                setDefaultCapsules();
            } else {
                //Otherwise, read the temporary save file
                try {
                    //Create the reader
                    BufferedReader reader = new BufferedReader(new FileReader(temp_save));

                    //Read in the number of capsules and the code that expresses the focused view
                    int capsule_list_size = Integer.parseInt(reader.readLine());
                    String current_focus_code = reader.readLine();

                    //Loop through each capsule, populating its fields
                    LinearLayout linearLayout = findViewById(R.id.capsules);
                    for (int i = 0; i < capsule_list_size; i++) {
                        //Add a capsule
                        AddCapsule();
                        //Find the capsule at i
                        LinearLayout temp_capsule = (LinearLayout) linearLayout.getChildAt(i);

                        //Read and add the label
                        EditText temp_label = temp_capsule.findViewById(R.id.label);
                        temp_label.setText(reader.readLine());
                        //Read and add the weighting
                        EditText temp_weighting = temp_capsule.findViewById(R.id.weight);
                        temp_weighting.setText(reader.readLine());
                        //Read and add the mark
                        EditText temp_mark = temp_capsule.findViewById(R.id.mark);
                        temp_mark.setText(reader.readLine());
                        //Read and add the total
                        EditText temp_total = temp_capsule.findViewById(R.id.total);
                        temp_total.setText(reader.readLine());
                    }

                    //Read and add the final
                    EditText temp_final = findViewById(R.id.final_mark);
                    temp_final.setText(reader.readLine());

                    //Break up the focus code into two strings, a number and a letter
                    String[] current_focus_code_split = current_focus_code.split(" ");
                    //If there is no letter (2nd element of array), the focus is set to Final
                    if (current_focus_code_split.length == 1) {
                        findViewById(R.id.final_mark).requestFocus();
                    }
                    //Otherwise, find the correct capsule
                    else {
                        //Get the capsule number
                        int capsule_number = Integer.parseInt(current_focus_code_split[0]);
                        //Go to the capsule with that number
                        LinearLayout temp_capsule = (LinearLayout) linearLayout.getChildAt(capsule_number - 1);

                        //Set focus to the view specified by the focus code letter
                        switch (current_focus_code_split[1]) {
                            case "w":
                                temp_capsule.findViewById(R.id.weight).requestFocus();
                                break;
                            case "m":
                                temp_capsule.findViewById(R.id.mark).requestFocus();
                                break;
                            case "t":
                                temp_capsule.findViewById(R.id.total).requestFocus();
                                break;
                            case "l":
                                temp_capsule.findViewById(R.id.label).requestFocus();
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_add:
                AddCapsule();
                return true;
            case R.id.action_reset:
                Reset();
                return true;
            case R.id.action_about:
                About();
                return true;
            case R.id.action_help:
                Help();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    ///////////////////////////////////////////////////////////////////

    private void Reset() {
        ResetDialog resetDialog = new ResetDialog();
        resetDialog.show(getSupportFragmentManager(), "reset");
    }

    public static class ResetDialog extends android.support.v4.app.DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Use a builder to create an alert dialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.reset_title)
                   .setMessage(R.string.reset_message)
                   .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           //Remove all capsules
                           ((MainActivity) getActivity()).removeAll();

                           //Add two capsules and set the default focus
                           ((MainActivity) getActivity()).setDefaultCapsules();

                           //Clear Final
                           EditText final_editText = getActivity().findViewById(R.id.final_mark);
                           final_editText.setText(null);
                       }
                   })
                   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           //Cancel
                       }
                   });

            return builder.create();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void Help() {
        hideKeyboard();

        //Inflate the Help popup
        final View popup_view = getLayoutInflater().inflate(R.layout.help_popup, new LinearLayout(getApplicationContext()), true);
        final PopupWindow help_popup = new PopupWindow(popup_view,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        help_popup.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.accent)));
        help_popup.setFocusable(true);
        help_popup.showAtLocation(findViewById(R.id.no_focus), Gravity.CENTER, 0, 0);

        //Listen for the Close button press to dismiss popup
        Button btn_close = popup_view.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                help_popup.dismiss();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private void About() {
        hideKeyboard();

        //Inflate the About popup
        final View popup_view = getLayoutInflater().inflate(R.layout.about_popup, new LinearLayout(getApplicationContext()), true);
        final PopupWindow about_popup = new PopupWindow(popup_view,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        about_popup.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.accent)));
        about_popup.setFocusable(true);
        about_popup.showAtLocation(findViewById(R.id.no_focus), Gravity.CENTER, 0, 0);

        //Listen for the Close button press to dismiss popup
        Button btn_close = popup_view.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                about_popup.dismiss();
            }
        });
    }

    public void goToWebsite(View view) {
        goToUrl("http://briantran.me");
    }

    public void goToGithub(View view) {
        goToUrl("https://github.com/TranBrian10/WeightedCalculator");
    }

    public void goToRate(View view) {
        Uri uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
        Intent goToRate = new Intent(Intent.ACTION_VIEW, uri);
        goToRate.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToRate);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id="
                            + getApplicationContext().getPackageName())));
        }
    }

    private void goToUrl(String url) {
        Uri link = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, link);
        startActivity(launchBrowser);
    }

    ///////////////////////////////////////////////////////////////////

    private void AddCapsule() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View capsuleLayout = inflater.inflate(R.layout.capsule_layout, new LinearLayout(getApplicationContext()), true);
        LinearLayout linearLayout = findViewById(R.id.capsules);
        linearLayout.addView(capsuleLayout);

        capsuleList.add(capsuleLayout);
        TextView textView = capsuleLayout.findViewById(R.id.capsule_number);
        textView.setText(String.valueOf(capsuleList.size()));

        //Scroll to bottom
        capsuleList.get(capsuleList.size()-1).findViewById(R.id.weight).requestFocus();
        final ScrollView scrollView = findViewById(R.id.capsules_scroll);
        scrollView.scrollTo(0, scrollView.getBottom());
    }

    public void Remove(View view) {
        hideKeyboard();

        //Get the capsule and list
        View parent_view = (View) view.getParent().getParent().getParent();
        LinearLayout linearLayout = findViewById(R.id.capsules);

        //Loop through following capsules and decrement number
        TextView textView = parent_view.findViewById(R.id.capsule_number);
        int loop_start_index = (Integer.parseInt(textView.getText().toString()));
        for (int i = loop_start_index; i < capsuleList.size(); i++) {
            LinearLayout temp_layout = (LinearLayout) linearLayout.getChildAt(i);
            TextView temp_textView = temp_layout.findViewById(R.id.capsule_number);
            temp_textView.setText(String.format(Locale.getDefault(), "%d", i));
        }

        //Set focus to the capsule above the removed capsule (the removed capsule is at loop_start_index - 1)
        if (loop_start_index != 1) {
            linearLayout.getChildAt(loop_start_index - 2).findViewById(R.id.weight).requestFocus();
        }
        //If the first capsule is removed, try to set the focus to the capsule below
        else if (capsuleList.size() > 1) {
            linearLayout.getChildAt(loop_start_index).findViewById(R.id.weight).requestFocus();
        }

        //Remove from capsule list
        int index = (Integer.parseInt(textView.getText().toString()) - 1);
        capsuleList.remove(index);

        //Remove from view
        linearLayout.removeView(parent_view);
    }

    private void removeAll() {
        hideKeyboard();

        //Remove all capsules, starting from the bottom
        LinearLayout linearLayout = findViewById(R.id.capsules);
        for (int i = capsuleList.size() - 1; i >= 0; i--) {
            LinearLayout capsule_layout = (LinearLayout) linearLayout.getChildAt(i);
            Remove(capsule_layout.findViewById(R.id.remove));
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void Calculate() {
        LinearLayout linearLayout = findViewById(R.id.capsules);
        EditText final_editText = findViewById(R.id.final_mark);
        double final_mark = 0;
        double total_weight = 0;
        boolean reverse_calculation = false;
        Double reverse_final = (double) 0;
        double reverse_total = 100;
        boolean reverse_total_default = true;
        Double reverse_weighting = (double) 0;
        EditText reverse_mark_editText = null;
        EditText reverse_total_editText = null;

        //Loop through each capsule
        //Get total weight
        for (int i = 0; i < capsuleList.size(); i++) {
            LinearLayout temp_layout = (LinearLayout) linearLayout.getChildAt(i);
            EditText temp_weighting = temp_layout.findViewById(R.id.weight);

            if (temp_weighting.getText().toString().length() > 0
                    && !temp_weighting.getText().toString().equals(".")) {
                Double weighting_value = edittextToDouble(temp_weighting);
                if (weighting_value == null) return;

                total_weight += weighting_value;
            }
            else {
                error_toast.setText(R.string.weighting_alert_toast);
                error_toast.show();

                return;
            }
        }
        //mark * (weight / total weight), add together
        for (int i = 0; i < capsuleList.size(); i++) {
            LinearLayout temp_layout = (LinearLayout) linearLayout.getChildAt(i);
            EditText temp_weighting = temp_layout.findViewById(R.id.weight);
            EditText temp_mark = temp_layout.findViewById(R.id.mark);

            //Check if mark is filled in
            if (temp_mark.getText().toString().length() > 0
                    && !temp_mark.getText().toString().equals(".")) {
                EditText temp_total = temp_layout.findViewById(R.id.total);

                //Check if total is filled in
                if (temp_total.getText().toString().length() > 0
                        && !temp_total.getText().toString().equals(".")) {
                    Double weighting_value = edittextToDouble(temp_weighting);
                    if (weighting_value == null) return;

                    Double mark_value = edittextToDouble(temp_mark);
                    if (mark_value == null) return;

                    Double total_value = edittextToDouble(temp_total);
                    if (total_value == null) return;


                    if (total_value > 0) {
                        final_mark += (weighting_value / total_weight) * (mark_value / total_value);
                    }
                    else {
                        error_toast.setText(R.string.total_zero_toast);
                        error_toast.show();

                        return;
                    }
                }
                else {
                    error_toast.setText(R.string.total_alert_toast);
                    error_toast.show();

                    return;
                }
            }
            else {
                //Could be a case of reverse calculation
                //Check if final is filled
                if (final_editText.getText().toString().length() > 0
                        && !final_editText.getText().toString().equals(".")
                        && !reverse_calculation) {
                    reverse_calculation = true;
                    reverse_mark_editText = temp_mark;
                    reverse_final = edittextToDouble(final_editText);
                    if (reverse_final == null) return;

                    EditText temp_total = temp_layout.findViewById(R.id.total);
                    reverse_total_editText = temp_total;

                    //Check if total is filled in
                    if (temp_total.getText().toString().length() > 0
                            && !temp_total.getText().toString().equals(".")) {
                        //Change reverse total from default and record the weighting
                        reverse_total_default = false;
                        Double total_value = edittextToDouble(temp_total);
                        if (total_value == null) return;

                        if (total_value > 0) {
                            reverse_total = total_value;
                        }
                        else {
                            error_toast.setText(R.string.total_zero_toast);
                            error_toast.show();

                            return;
                        }
                    }

                    reverse_weighting = edittextToDouble(temp_weighting);
                    if (reverse_weighting == null) return;
                }
                else {
                    error_toast.setText(R.string.mark_final_alert_toast);
                    error_toast.show();

                    return;
                }
            }
        }

        //Display rounded reverse calculation mark
        if (reverse_calculation) {
            double reverse_mark = (reverse_final / 100) - (final_mark);
            reverse_mark = (reverse_mark / (reverse_weighting / total_weight)) * reverse_total;

            try {
                reverse_mark_editText.setText(String.format(Locale.getDefault(), "%.2f", reverse_mark));
                //Set the total to 100 if it is not filled in
                if (reverse_total_default) {
                    reverse_total_editText.setText(String.format(Locale.getDefault(), "%.2f", reverse_total));
                }
            }
            catch (NullPointerException e) {
                error_toast.setText(R.string.double_parsing_error_toast);
                error_toast.show();
            }

            reverse_mark_editText.requestFocus();
        }
        //Display rounded final mark
        else {
            final_mark = final_mark * 100;

            try {
                final_editText.setText(String.format(Locale.getDefault(), "%.2f", final_mark));
            }
            catch (NullPointerException e) {
                error_toast.setText(R.string.double_parsing_error_toast);
                error_toast.show();
            }
        }
    }

    //Converts a string to a positive double, taking into account the user's locale
    //Returns null if error
    private Double edittextToDouble(EditText et) {
        String string = et.getText().toString();
        String dotString = string.replaceAll(",", ".");

        //Count the number of decimal separators in the string
        int count = 0;
        for (int i = 0; i < dotString.length(); i++) {
            if (dotString.charAt(i) == '.') {
                count++;
            }
        }

        if (count > 1) {
            et.requestFocus();

            error_toast.setText(R.string.decimal_separator_error_toast);
            error_toast.show();

            return null;
        }

        return Double.parseDouble(dotString);
    }

    ///////////////////////////////////////////////////////////////////

    //Hides the soft keyboard
    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
    }
}