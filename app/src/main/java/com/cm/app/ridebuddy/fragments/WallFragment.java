package com.cm.app.ridebuddy.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.firebase.Exercise;
import com.cm.app.ridebuddy.firebase.Friend;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class WallFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private OnListFragmentInteractionListener mListener;
    SwipeRefreshLayout swipeLayout;
    protected RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<Exercise, ExerciseViewHolder>
            mFirebaseAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private LinearLayoutManager mLinearLayoutManager;
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 2;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private NavigationView navigationView;


    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected LayoutManagerType mCurrentLayoutManagerType;


    /**
     * This is the standard support library way of implementing "swipe to delete" feature. You can do custom drawing in onChildDraw method
     * but whatever you draw will disappear once the swipe is over, and while the items are animating to their new position the recycler view
     * background will be visible. That is rarely an desired effect.
     */
    private void setUpItemTouchHelper() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                Log.d(TAG, "init: HALP");
                background = new ColorDrawable(Color.RED);
              //  xMark = ContextCompat.getDrawable(getContext(), R.drawable.cast_ic_expanded_controller_play);
              //  xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                //  xMarkMargin = (int) getActivity().getResources().getDimension(R.dimen.ic_clear_margin);
                xMarkMargin = 50;
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                FirebaseRecyclerAdapter testAdapter = (FirebaseRecyclerAdapter) recyclerView.getAdapter();
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int swipedPosition = viewHolder.getAdapterPosition();
                final FirebaseRecyclerAdapter adapter = (FirebaseRecyclerAdapter) mRecyclerView.getAdapter();
                final Exercise exercise = (Exercise) adapter.getItem(swipedPosition);
                if(!exercise.getgID().equals(mFirebaseUser.getUid())){
                    MyUtils.INSTANCE.showToast(getContext(),"Can't delete, exercise not yours!");
                    adapter.notifyDataSetChanged();
                    return;
                }
                new AlertDialog.Builder(getContext())
                        .setTitle("RideBuddy")
                        .setMessage("Do you really want to remove this exercise?")
                        .setIcon(R.drawable.bikeorange)
                        .setPositiveButton(android.R.string.yes,new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                mDatabase.child("exercises").child(exercise.getMyID()).removeValue();
                                mDatabase.child("user_wall").child(mFirebaseUser.getUid()).child(exercise.getMyID()).removeValue();

                                //aqui iterar todos os amigos e apagar do mural


                                Query queryRefm =  mDatabase.child("user_friends").child(mFirebaseUser.getUid()).orderByChild("userName");
                                queryRefm.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                                            Friend myFriend = mdataSnapshot.getValue(Friend.class);
                                            mDatabase.child("user_wall").child(myFriend.getUid()).child(exercise.getMyID()).removeValue();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        MyUtils.INSTANCE.showToast(getContext(),"DB ERROR "+ databaseError);
                                    }
                                });

                                adapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Exercise Removed", Toast.LENGTH_SHORT).show();
                                deleteImageFirebase(exercise.getSnapshotname()+".jpg");
                            }} )
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                adapter.notifyDataSetChanged();
                            }}).show();
                 //   adapter.remove(swipedPosition);
                Log.d(TAG, "onSwiped: removed");

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }



                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    /**
     * We're gonna setup another ItemDecorator that will draw the red background in the empty space while the items are animating to thier new positions
     * after an item is removed.
     */
    private void setUpAnimationDecoratorHelper() {
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.LTGRAY);
                initiated = true;
            }

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (parent.getItemAnimator().isRunning()) {

                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (child.getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        // views are coming down AND going up to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        // views are going down to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        // views are coming up to fill the void
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
    }


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WallFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        update_my_wall();
        View rootView = inflater.inflate(R.layout.fragment_wall_list, container, false);
        // mRecyclerView = (RecyclerView) rootView.findViewById(myRecycler);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.myRecycler);
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        //mLinearLayoutManager.setReverseLayout(true);
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Exercise, ExerciseViewHolder>(
                Exercise.class,
                R.layout.fragment_wall,
                ExerciseViewHolder.class,
                mDatabase.child("user_wall").child(mFirebaseUser.getUid()).orderByChild("date")) {

            @Override
            protected void populateViewHolder(ExerciseViewHolder holder,
                                              final Exercise exercise, int position) {


                    holder.mItem = exercise;
                    holder.mNameView.setText(exercise.getUserName());

                    Glide.with(getContext()).load("https://lh4.googleusercontent.com" + exercise.getPhotoURL())
                            .crossFade()
                            .thumbnail(0.5f)
                            .bitmapTransform(new CircleTransform(getContext()))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.mThumbView);

                    holder.mExerciseNameView.setText(exercise.getName());


                    holder.mMetaView.setText(new Date(exercise.getDate()).toString());

                    double kilometers = exercise.getTotalDistance() / 1000;
                    double kilometersRound = Math.round(kilometers * 1000.0) / 1000.0;

                    double elevation = Math.round(exercise.getElevationGain());

                    String rideData = "Distance: " + kilometersRound + " Km  Elevation gain: "
                            + elevation + " m";
                    holder.mRideDataView.setText(rideData);


                    // Reference to an image file in Firebase Storage
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageReference =
                            storage.getReferenceFromUrl("gs://ridebuddy-cb318.appspot.com/images/" + exercise.getSnapshotname() + ".jpg");


                    // Load the image using Glide
                    Glide.with(getContext() /* context */)
                            .using(new FirebaseImageLoader())
                            .load(storageReference)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.mRideImgView);

                    holder.mView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (null != mListener) {
                                // Notify the active callbacks interface
                                mListener.onListFragmentInteraction(exercise);
                            }
                        }
                    });

                }

        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }

            }
        });



        mLinearLayoutManager.setReverseLayout(true);
        mLinearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mFirebaseAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);

        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();

        return rootView;
    }

    public void update_my_wall(){
        Log.d(TAG, "onDataChange: update my wall");



        DatabaseReference myRefm = FirebaseDatabase.getInstance().getReference("exercises");
        Query queryRefm = myRefm.orderByChild("gID").equalTo(mFirebaseUser.getUid());
        queryRefm.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                    Exercise exercise = mdataSnapshot.getValue(Exercise.class);
                    mDatabase.child("user_wall").child(mFirebaseUser.getUid()).child(exercise.getMyID()).setValue(exercise);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        DatabaseReference myRefm2 = FirebaseDatabase.getInstance().getReference("user_friends");
        Query queryRefm2 = myRefm2.child(mFirebaseUser.getUid()).orderByChild("uid");
        queryRefm2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {

                    Friend friend = mdataSnapshot.getValue(Friend.class);
                    DatabaseReference myRefm3 = FirebaseDatabase.getInstance().getReference("exercises");
                    Query queryRefm3 = myRefm3.orderByChild("gID").equalTo(friend.getUid());
                    Log.d(TAG, "onDataChange: update my wall2");
                    queryRefm3.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                                Exercise exercise = mdataSnapshot.getValue(Exercise.class);
                                mDatabase.child("user_wall").child(mFirebaseUser.getUid()).child(exercise.getMyID()).setValue(exercise);

                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });


                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


    }

    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }


        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;


        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;

        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        //  mAdapter.setNewData(qb.list());
        update_my_wall();
      //  navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
      //  navigationView.getMenu().getItem(0).setChecked(true);
        // mFirebaseAdapter.notifyDataSetChanged();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

    }

    @Override
    public void onRefresh() {

        // mAdapter.notifyDataSetChanged();
        //MyUtils.INSTANCE.showToast(getContext(), "Wall Refreshed");
        update_my_wall();
      //  mFirebaseAdapter.notifyDataSetChanged();
        swipeLayout.setRefreshing(false);

    }


    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Exercise item);
    }

    private void deleteImageFirebase(String img){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://ridebuddy-cb318.appspot.com");
        StorageReference imagesRef = storageRef.child("images");





// Delete the file
        imagesRef.child(img).delete().addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
              //  MyUtils.INSTANCE.showToast(getContext(),"img");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });
    }

}
