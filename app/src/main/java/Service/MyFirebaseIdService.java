package Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import Common.Common;
import Model.Token;

public class MyFirebaseIdService extends FirebaseInstanceIdService {
        @Override
        public void onTokenRefresh() {
            super.onTokenRefresh();
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        Common.currentToken =refreshedToken; // no need to use this
            updateTokenToServer(refreshedToken);
        }

        private void updateTokenToServer(String refreshedToken) {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference tokens =db.getReference(Common.tokens_tbl);
            Token token =  new Token(refreshedToken);
            if (FirebaseAuth.getInstance().getCurrentUser()!=null) //if already logged in, must update token
                tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(token);

        }
    }
