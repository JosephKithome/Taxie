package com.example.taxie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import Common.Common;
import Model.Riders;
import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    Button btnSignIn, btnRegister;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference riders;
    RelativeLayout rootLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //        initialize firebase
        auth =FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();
        riders= db.getReference(Common.user_rider_tbl);

//        initialize view
        btnSignIn=(Button)findViewById(R.id.btnSignin);
        btnRegister=(Button)findViewById(R.id.btnSignup);
        rootLayout=(RelativeLayout)findViewById(R.id.rootLayout) ;
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });

    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog= new AlertDialog.Builder(this);
        dialog.setTitle("Login");
        dialog.setMessage("Please use email to login");

        LayoutInflater inflater=LayoutInflater.from(this);
        View login_layout =inflater.inflate(R.layout.layout_login,null);
        final MaterialEditText edtEmail= login_layout.findViewById(R.id.edt_email);
        final MaterialEditText edtpassword= login_layout.findViewById(R.id.edt_password);

        dialog.setView(login_layout);

//        set button
        dialog.setPositiveButton("SignIn", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
//                set disable button signIn if processing
                btnSignIn.setEnabled(false);

//                check validation
                if(TextUtils.isEmpty(edtEmail.getText().toString())){
                    Snackbar.make(rootLayout,"Please enter email address",Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if(edtpassword.getText().toString().length()< 6 ){
                    Snackbar.make(rootLayout,"Password too short",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                final android.app.AlertDialog waitingDialog = new  SpotsDialog.Builder().setContext(MainActivity.this).setCancelable(false).build();
                waitingDialog.show();
//               sign in user
                auth.signInWithEmailAndPassword(edtEmail.getText().toString(),edtpassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();
                                startActivity(new Intent(MainActivity.this,HomeActivity.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingDialog.dismiss();
                        Snackbar.make(rootLayout,"Failed"+e.getMessage(),Snackbar.LENGTH_SHORT)
                                .show();
//                       Active button
                        btnSignIn.setEnabled(true);
                    }
                });

            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog= new AlertDialog.Builder(this);
        dialog.setTitle("Register");
        dialog.setMessage("Please use email to register");

        LayoutInflater inflater=LayoutInflater.from(this);
        View register_Layout =inflater.inflate(R.layout.layout_register,null);
        final MaterialEditText edtEmail= register_Layout.findViewById(R.id.edt_email);
        final MaterialEditText edtpassword= register_Layout.findViewById(R.id.edt_password);
        final MaterialEditText edtname= register_Layout.findViewById(R.id.edt_name);
        final MaterialEditText edtphone= register_Layout.findViewById(R.id.edt_phone);
        dialog.setView(register_Layout);

//        set button
        dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

//                check validation
                if(TextUtils.isEmpty(edtEmail.getText().toString())){
                    Snackbar.make(rootLayout,"Please enter email address",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edtphone.getText().toString())){
                    Snackbar.make(rootLayout,"Please enter phone number",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edtname.getText().toString())){
                    Snackbar.make(rootLayout,"Please enter name",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(edtpassword.getText().toString().length()< 6 ){
                    Snackbar.make(rootLayout,"Password too short",Snackbar.LENGTH_SHORT).show();
                    return;
                }
//                register new user
                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(),edtpassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {

//                                save user to db
                                Riders rider = new Riders();
                                rider.setEmail(edtEmail.getText().toString());
                                rider.setName(edtname.getText().toString());
                                rider.setPassword(edtpassword.getText().toString());
                                rider.setPhone(edtphone.getText().toString());

                                riders.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(rider)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(rootLayout," Register Successful",Snackbar.LENGTH_SHORT).show();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(rootLayout,"Failed"+e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });




                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(rootLayout,"Failed"+e.getMessage(),Snackbar.LENGTH_SHORT).show();
                    }
                });

            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();




    }
}