package com.rxjava.demos;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.anshul.rxdownloader.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.internal.util.ErrorMode;
import io.reactivex.schedulers.Timed;

/**
 * Created by anshul on 29/1/17. Modifed By Gulzar :D
 */

public class FbLiveVideoReactionDemoActivity extends AppCompatActivity {
  private Subscription emoticonSubscription;
  private Subscriber subscriber;
  private final int MINIMUM_DURATION_BETWEEN_EMOTICONS = 300; // in milliseconds
  boolean[] skip=new boolean[6];

  public
  // Emoticons views.
  @BindView(R.id.like_emoticon)
  ImageView likeEmoticonButton;
  @BindView(R.id.love_emoticon)
  ImageView loveEmoticonButton;
  @BindView(R.id.haha_emoticon)
  ImageView hahaEmoticonButton;
  @BindView(R.id.wow_emoticon)
  ImageView wowEmoticonButton;
  @BindView(R.id.sad_emoticon)
  ImageView sadEmoticonButton;
  @BindView(R.id.angry_emoticon)
  ImageView angryEmoticonButton;

  @BindView(R.id.custom_view)
  EmoticonsView emoticonsView;

  public DatabaseReference mDatabase;

  private Animation emoticonClickAnimation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //Set the view and do all the necessary init.
    setContentView(R.layout.activity_fb_live_video_reaction_demo);
    mDatabase= FirebaseDatabase.getInstance().getReference();
    ButterKnife.bind(this);


  }

  @Override
  public void onStart() {
    super.onStart();
    //SKIPS FIRST READ
    for(int i=0;i<skip.length;i++)
      skip[i]=true;

    //Create an instance of FlowableOnSubscribe which will convert click events to streams
    FlowableOnSubscribe flowableOnSubscribe = new FlowableOnSubscribe() {
      @Override
      public void subscribe(final FlowableEmitter emitter) throws Exception {
        convertClickEventToStream(emitter);
        //Reads data on Change
        readData(emitter);
      }
    };
    //Give the backpressure strategy as BUFFER, so that the click items do not drop.
    Flowable emoticonsFlowable = Flowable.create(flowableOnSubscribe, BackpressureStrategy.BUFFER);
    //Convert the stream to a timed stream, as we require the timestamp of each event
    Flowable<Timed> emoticonsTimedFlowable = emoticonsFlowable.timestamp();
    subscriber = getSubscriber();
    //Subscribe
    emoticonsTimedFlowable.subscribeWith(subscriber);
  }

  private Subscriber getSubscriber() {
    return new Subscriber<Timed<Emoticons>>() {
      @Override
      public void onSubscribe(Subscription s) {
        emoticonSubscription = s;
        emoticonSubscription.request(1);

        // for lazy evaluation.
        emoticonsView.initView(FbLiveVideoReactionDemoActivity.this);
      }

      @Override
      public void onNext(final Timed<Emoticons> timed) {

        emoticonsView.addView(timed.value());

        long currentTimeStamp = System.currentTimeMillis();
        long diffInMillis = currentTimeStamp - ((Timed) timed).time();
        if (diffInMillis > MINIMUM_DURATION_BETWEEN_EMOTICONS) {
          emoticonSubscription.request(1);
        } else {
          Handler handler = new Handler();
          handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              emoticonSubscription.request(1);
            }
          }, MINIMUM_DURATION_BETWEEN_EMOTICONS - diffInMillis);
        }
      }

      @Override
      public void onError(Throwable t) {
        //Do nothing
      }

      @Override
      public void onComplete() {
        if (emoticonSubscription != null) {
          emoticonSubscription.cancel();
        }
      }
    };
  }

  @Override
  public void onStop() {
    super.onStop();
    if (emoticonSubscription != null) {
      emoticonSubscription.cancel();
    }
  }


  private void convertClickEventToStream(final FlowableEmitter emitter) {
    likeEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(likeEmoticonButton, emitter, Emoticons.LIKE);
        fireData(Emoticons.LIKE.toString());

      }
    });

    loveEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(loveEmoticonButton, emitter, Emoticons.LOVE);
        fireData(Emoticons.LOVE.toString());
      }
    });

    hahaEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(hahaEmoticonButton, emitter, Emoticons.HAHA);
        fireData(Emoticons.HAHA.toString());
      }
    });

    wowEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(wowEmoticonButton, emitter, Emoticons.WOW);
        fireData(Emoticons.WOW.toString());
      }
    });

    sadEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(sadEmoticonButton, emitter, Emoticons.SAD);
        fireData(Emoticons.SAD.toString());
      }
    });

    angryEmoticonButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        doOnClick(angryEmoticonButton, emitter, Emoticons.ANGRY);
        fireData(Emoticons.ANGRY.toString());
      }
    });
  }

  private void doOnClick(View view, FlowableEmitter emitter, Emoticons emoticons) {
    emoticonClickAnimation = AnimationUtils.loadAnimation(FbLiveVideoReactionDemoActivity
        .this, R.anim.emoticon_click_animation);
    view.startAnimation(emoticonClickAnimation);
  }

  private void fireData(String emoticon)
  {
    String key=mDatabase.child(emoticon+"").push().getKey();
    mDatabase.child(emoticon+"").child(key).setValue(true);
  }

  private void readData(final FlowableEmitter emitter)
  {


    mDatabase.child(Emoticons.LIKE.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[0])
        emitter.onNext(Emoticons.LIKE);
        skip[0]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
    mDatabase.child(Emoticons.LOVE.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[1])
        emitter.onNext(Emoticons.LOVE);
        skip[1]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
    mDatabase.child(Emoticons.WOW.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[2])
        emitter.onNext(Emoticons.WOW);
        skip[2]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
    mDatabase.child(Emoticons.HAHA.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[3])
        emitter.onNext(Emoticons.HAHA);
        skip[3]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
    mDatabase.child(Emoticons.ANGRY.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[4])
        emitter.onNext(Emoticons.ANGRY);
        skip[4]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });
    mDatabase.child(Emoticons.SAD.toString()).addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if(!skip[5])
        emitter.onNext(Emoticons.SAD);
        skip[5]=false;
      }
      @Override
      public void onCancelled(DatabaseError databaseError) {

      }
    });

  }
}
