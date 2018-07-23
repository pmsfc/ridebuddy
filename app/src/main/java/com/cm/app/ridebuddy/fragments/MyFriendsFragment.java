package com.cm.app.ridebuddy.fragments;


import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.Friend;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;


public class MyFriendsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {


    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    SwipeRefreshLayout swipeLayout;
    protected RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<Friend, FriendViewHolder>
            mFirebaseAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private LinearLayoutManager mLinearLayoutManager;
    private NavigationView navigationView;

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected MyFriendsFragment.LayoutManagerType mCurrentLayoutManagerType;


    public MyFriendsFragment() {
        // Required empty public constructor
    }


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
                final Friend friend = (Friend) adapter.getItem(swipedPosition);
                new AlertDialog.Builder(getContext())
                        .setTitle("RideBuddy")
                        .setMessage("Delete this Friend?")
                        .setIcon(R.drawable.bikeorange)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                //delete friends
                                mDatabase.child("user_friends").child(mFirebaseUser.getUid()).child(friend.getUid()).removeValue();
                                mDatabase.child("user_friends").child(friend.getUid()).child(mFirebaseUser.getUid()).removeValue();
                                //update their wall
                                mDatabase.child("user_wall").child(friend.getUid()).removeValue();
                                mDatabase.child("user_wall").child(mFirebaseUser.getUid()).removeValue();

                                adapter.notifyDataSetChanged();

                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                adapter.notifyDataSetChanged();
                            }
                        }).show();
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_my_friends, container, false);
        // mRecyclerView = (RecyclerView) rootView.findViewById(myRecycler);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.myRecycler);
        mCurrentLayoutManagerType = MyFriendsFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (MyFriendsFragment.LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        //mLinearLayoutManager.setReverseLayout(true);
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Friend, FriendViewHolder>(
                Friend.class,
                R.layout.fragment_my_friends_item,
                FriendViewHolder.class,
                mDatabase.child("user_friends").child(mFirebaseUser.getUid()).orderByChild("userName")) {

            @Override
            protected void populateViewHolder(FriendViewHolder holder,
                                              final Friend friend, int position) {

                holder.mItem = friend;
                holder.mNameView.setText(friend.getUserName());

                Glide.with(getContext()).load("https://lh4.googleusercontent.com" + friend.getPhoto())
                        .crossFade()
                        .thumbnail(0.5f)
                        .error(R.drawable.bikeorange)
                        .bitmapTransform(new CircleTransform(getContext()))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.mThumbView);


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
        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);

        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
        getActivity().setTitle("My Friends");
    }



    public void setRecyclerViewLayoutManager(MyFriendsFragment.LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }


        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = MyFriendsFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;


        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }


    @Override
    public void onResume() {
        super.onResume();

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
       // fab.setImageResource(R.drawable.ic_action_add);
  //      navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
//        navigationView.getMenu().getItem(1).setChecked(true);


    }



    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onRefresh() {

        // mAdapter.notifyDataSetChanged();
      //  MyUtils.INSTANCE.showToast(getContext(), "Refreshed");
        mFirebaseAdapter.notifyDataSetChanged();
        swipeLayout.setRefreshing(false);

    }


}
