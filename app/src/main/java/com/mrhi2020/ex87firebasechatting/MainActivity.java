package com.mrhi2020.ex87firebasechatting;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    EditText et;
    CircleImageView civ;

    Uri imgUri; //선택된 이미지의 콘텐츠 주소(경로)

    boolean isFirst= true; //처음 앱을 실행하여 프로필 데이터가 없는가?
    boolean isChanged= false;//기존 프로필이미지를 변경한 적인 있는가?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et= findViewById(R.id.et);
        civ= findViewById(R.id.iv);

        //SharedPreferences 에 미리 저장되어 있는 닉네임, 프로필이미지가 있다면 읽어와라..
        loadData();
        if(G.nickName != null){
            et.setText(G.nickName);
            Picasso.get().load(G.profileUrl).into(civ);

            //처음이 아니네..
            isFirst= false;
        }
    }

    //SharedPreferences 의 저장값들 읽어오는 기능 메소드
    void loadData(){
        SharedPreferences pref= getSharedPreferences("account", MODE_PRIVATE);
        G.nickName= pref.getString("nickName", null);
        G.profileUrl= pref.getString("profileUrl", null);
    }

    public void clickImage(View view) {
        //사진 or 갤러리 앱에서 사진 선택하도록
        Intent intent= new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==10 && resultCode==RESULT_OK){
            imgUri= data.getData();
            Picasso.get().load(imgUri).into(civ);//Picasso 는 외부저장소 동적퍼미션이 자동 적용됨

            //사진이 변경되었으므로
            isChanged= true;
        }
    }

    public void clickBtn(View view) {

        //처음 이거나 사진이 변경되었다면?
        if(isFirst || isChanged){
            //닉네임과 프로필이미지를 Firebase storage와 DB에 저장 후 채팅화면으로 이동 - 서버에 저장해야 다른 폰에서도 이미지가 보여짐
            saveData();
        }else{
            //저장 없이 곧바로 채팅화면으로 전환
            Intent intent= new Intent(this, ChattingActivity.class);
            startActivity(intent);
            finish();
        }

    }

    //닉네임과 프로필이미지를 저장하는 기능 메소드
    void saveData(){

        //EditText에 작성한 닉네임 가져오기
        G.nickName= et.getText().toString();

        //이미지 선택을 혹시 안하면 채팅 못하도록..
        if(imgUri==null) return;

        //우선 이미지 업로드가 오래거리므로 먼저 수행
        //FirebaseStorage 에 먼저 업로드

        //fire storage 에 중복된 파일명이 있으면 안돼서..
        SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName= sdf.format(new Date()) + ".png";

        //firebase storage에 이미지 업로드
        FirebaseStorage firebaseStorage= FirebaseStorage.getInstance();
        StorageReference imgRef= firebaseStorage.getReference("profileImage/"+fileName);

        UploadTask uploadTask= imgRef.putFile(imgUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //업로드 된 파일의 다운로드 주소(서버에 있는 이미지의 인터넷경로 URL)를 얻어오도록
                imgRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //firebase 저장소에 저장되어 있는 이미지에 대한
                        //다운로드 주소 url을 문자열로 얻어오기
                        G.profileUrl= uri.toString();
                        Toast.makeText(MainActivity.this, "프로필 이미지 저장 완료 \n" + G.profileUrl, Toast.LENGTH_SHORT).show();

                        //1. 서버의 DB에 닉네임과 이미지url을 저장
                        //Firebase Database에 저장
                        FirebaseDatabase firebaseDatabase= FirebaseDatabase.getInstance();
                        //'profiles'라는 이름의 자식노드 참조(없으면 생성, 있으면 참조)
                        DatabaseReference profilesRef= firebaseDatabase.getReference("profiles");

                        //닉네임을 '키'값으로 하고 '값'을 이미지경로로 저장
                        profilesRef.child(G.nickName).setValue(G.profileUrl);


                        // 앱을 처음 실행할때만 닉네임과 사진을 입력하도록 하기위해
                        //2. SharedPreferences 에 저장 (내부저장소에 데이터를 영구히 저장하는 녀석)
                        SharedPreferences pref= getSharedPreferences("account", MODE_PRIVATE);
                        SharedPreferences.Editor editor= pref.edit();

                        editor.putString("nickName", G.nickName);
                        editor.putString("profileUrl", G.profileUrl);

                        editor.commit();

                        //저장이 완료되었으니 채팅화면으로 전환
                        Intent intent= new Intent(MainActivity.this, ChattingActivity.class);
                        startActivity(intent);

                        finish();
                    }
                });


            }
        });




    }


}