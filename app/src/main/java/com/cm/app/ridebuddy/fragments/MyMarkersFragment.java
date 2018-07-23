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
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.ExerciseMapMarker;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyMarkersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private MyMarkersFragment.OnListFragmentInteractionListener mListener;


    SwipeRefreshLayout swipeLayout;
    protected RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<ExerciseMapMarker, MarkersViewHolder>
            mFirebaseAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private LinearLayoutManager mLinearLayoutManager;
    private NavigationView navigationView;

    private enum LayoutManagerType {
        LINEAR_LAYOUT_MANAGER
    }

    protected MyMarkersFragment.LayoutManagerType mCurrentLayoutManagerType;


    public MyMarkersFragment() {
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
                final ExerciseMapMarker exerciseMapMarker = (ExerciseMapMarker) adapter.getItem(swipedPosition);
                new AlertDialog.Builder(getContext())
                        .setTitle("RideBuddy")
                        .setMessage("Delete this Marker?")
                        .setIcon(R.drawable.bikeorange)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                //delete here
                                mDatabase.child("map_markers").child(exerciseMapMarker.getMarkerID()).removeValue();
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

        View rootView = inflater.inflate(R.layout.fragment_my_markers, container, false);
        // mRecyclerView = (RecyclerView) rootView.findViewById(myRecycler);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.myRecycler);
        mCurrentLayoutManagerType = MyMarkersFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;

        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (MyMarkersFragment.LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        //mLinearLayoutManager.setReverseLayout(true);
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ExerciseMapMarker, MarkersViewHolder>(
                ExerciseMapMarker.class,
                R.layout.fragment_my_markers_item,
                MarkersViewHolder.class,
                mDatabase.child("map_markers").orderByChild("userID").equalTo(mFirebaseUser.getUid())) {

            @Override
            protected void populateViewHolder(MarkersViewHolder holder,
                                              final ExerciseMapMarker exerciseMapMarker, int position) {

                holder.mItem = exerciseMapMarker;
                holder.mNameView.setText(exerciseMapMarker.getMarkerName());


                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Notify the active callbacks interface
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());

                        // Set up the input
                        final EditText input = new EditText(getContext());


                        input.setText(exerciseMapMarker.getMarkerName());
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
                        builder.setView(input);

                        // Set up the buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String nName = input.getText().toString();
                                mDatabase.child("map_markers").child(exerciseMapMarker.getMarkerID())
                                        .child("markerName").setValue(nName);

                            }
                        });

                        // Set up the buttons
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        builder.setCancelable(true);
                        input.setSelection(input.length());


                        //  builder.setCancelable(false);


                        builder.show();

                    }
                });

                Glide.with(getContext()).load(R.drawable.bikeorangeblack)
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
        getActivity().setTitle("My Markers");
    }


    public void setRecyclerViewLayoutManager(MyMarkersFragment.LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }


        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = MyMarkersFragment.LayoutManagerType.LINEAR_LAYOUT_MANAGER;


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

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(ExerciseMapMarker item);
    }


}
