package com.cm.app.ridebuddy.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.firebase.Friend;
import com.cm.app.ridebuddy.firebase.User;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;


public class AddFriendFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {


    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    SwipeRefreshLayout swipeLayout;
    protected RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<User, UserViewHolder>
            mFirebaseAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private LinearLayoutManager mLinearLayoutManager;

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected AddFriendFragment.LayoutManagerType mCurrentLayoutManagerType;


    public AddFriendFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_add_friend, container, false);
        // mRecyclerView = (RecyclerView) rootView.findViewById(myRecycler);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.myRecycler);
        mCurrentLayoutManagerType = AddFriendFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (AddFriendFragment.LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        //mLinearLayoutManager.setReverseLayout(true);
        mFirebaseAdapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(
                User.class,
                R.layout.fragment_add_friend_item,
                UserViewHolder.class,
                mDatabase.child("users").orderByChild("username")) {

            @Override
            protected void populateViewHolder(UserViewHolder holder,
                                              final User user, int position) {


                // de modo a nao deixar adicionar sera necessario user preferences
                // isto so para janeiro !

            /*    if(mDatabase.child("user_friends").child(user.getUsername()).getRoot() != null){
                    holder.myButton.setText("Friends");
                    holder.myButton.setTextColor(Color.BLUE);
                    holder.myButton.setEnabled(false);
                }*/


                if(user.getEmail().equalsIgnoreCase(mFirebaseUser.getEmail())){
                    holder.myButton.setText("Me");
                    holder.myButton.setTextColor(Color.LTGRAY);
                    holder.myButton.setEnabled(false);

                }




                holder.mItem = user;
                holder.mNameView.setText(user.getUsername());

                Glide.with(getContext()).load("https://lh4.googleusercontent.com" + user.getPhotoURL())
                        .crossFade()
                        .thumbnail(0.5f)
                        .error(R.drawable.bikeorange)
                        .bitmapTransform(new CircleTransform(getContext()))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.mThumbView);



                holder.myButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addFriend(user);
                        MyUtils.INSTANCE.showToast(getContext(),"Friend added!");
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



      //  mLinearLayoutManager.setReverseLayout(true);
      //  mLinearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mFirebaseAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(10);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
       // swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
       // swipeLayout.setOnRefreshListener(this);

        //setUpItemTouchHelper();
        //setUpAnimationDecoratorHelper();

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();



    }


    public void setRecyclerViewLayoutManager(AddFriendFragment.LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }


        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = AddFriendFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;


        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }



    @Override
    public void onResume() {
        super.onResume();

        //  mAdapter.setNewData(qb.list());
        mFirebaseAdapter.notifyDataSetChanged();

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onRefresh() {

        // mAdapter.notifyDataSetChanged();
       // MyUtils.INSTANCE.showToast(getContext(), "Refreshed");
        mFirebaseAdapter.notifyDataSetChanged();
        swipeLayout.setRefreshing(false);

    }





    private void addFriend (User user){
        //i am friend of a friend of mine !
        Friend friend = new Friend(user.getPhotoURL(),user.getUserID(),new Date().getTime(),user.getUsername());
        Friend friend2 = new Friend(mFirebaseUser.getPhotoUrl().getPath(),mFirebaseUser.getUid(),new Date().getTime(),mFirebaseUser.getDisplayName());
        mDatabase.child("user_friends").child(mFirebaseUser.getUid()).child(user.getUserID()).setValue(friend);
        mDatabase.child("user_friends").child(user.getUserID()).child(mFirebaseUser.getUid()).setValue(friend2);

    }
}
