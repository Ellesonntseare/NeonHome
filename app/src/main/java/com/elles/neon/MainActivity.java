package com.elles.neon;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(230,244,250), muted = Color.rgb(144,174,191);
    private int accent = Color.rgb(112,238,250);
    private LinearLayout shell, content, drawerList;
    private TextView clock, date, count;
    private EditText search;
    private boolean drawer;
    private boolean privateUnlocked;
    private final Set<String> hiddenPackages = new HashSet<>();
    private AlertDialog privateDialog;
    private SharedPreferences prefs;
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<String> pinned = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int loadGeneration;
    private final Runnable tick = new Runnable() { public void run() { updateClock(); handler.postDelayed(this,1000); } };
    private static class App { String label, key; ComponentName component; ResolveInfo info; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("neon", MODE_PRIVATE);
        hiddenPackages.addAll(prefs.getStringSet("hiddenPackages",Collections.emptySet()));
        accent = prefs.getInt("accent", accent);
        String saved = prefs.getString("pinned", "");
        if (!saved.isEmpty()) pinned.addAll(Arrays.asList(saved.split("\\|")));
        buildRoot();
        showHome();
    }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(tick); handler.post(tick); loadApps(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); if(privateDialog!=null)privateDialog.dismiss(); if(privateUnlocked)showHome(); super.onPause(); }
    @Override protected void onDestroy() { loadGeneration++; handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); showHome(); }
    @Override public void onBackPressed() { if (drawer || privateUnlocked) showHome(); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("tnum"); return t;
    }
    private GradientDrawable surface(int fill, int border, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),border); return d;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void gap(LinearLayout l,int size) { View v = new View(this); l.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    private TextView button(String label, Runnable action) {
        TextView t = text(label,14,accent); t.setGravity(Gravity.CENTER); t.setPadding(dp(12),dp(14),dp(12),dp(14));
        t.setBackground(surface(0xBB0B1A28,0xFF254B5D,18)); t.setOnClickListener(v -> action.run()); return t;
    }
    private void buildRoot() {
        FrameLayout root = new FrameLayout(this); root.addView(new SpaceBackdrop(), new FrameLayout.LayoutParams(-1,-1));
        shell = vertical(); root.addView(shell,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v,insets) -> { Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime()); shell.setPadding(i.left,i.top,i.right,i.bottom); return insets; });
        }
    }
    private void hideKeyboard() { if(search != null) ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
    private void showHome() {
        hideKeyboard(); drawer=false; privateUnlocked=false; getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE); shell.removeAllViews(); search=null;
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); shell.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        content=vertical(); content.setPadding(dp(24),dp(12),dp(24),dp(20)); scroll.addView(content);
        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView word=text("N E O N",12,accent); word.setOnLongClickListener(v -> { unlockPrivate(); return true; }); brand.addView(word,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=button("Settings",this::settings); brand.addView(settings); content.addView(brand);
        gap(content,28); date=text("",15,muted); content.addView(date);
        clock=text("",76,accent); clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL)); clock.setSingleLine(true); clock.setAutoSizeTextTypeUniformWithConfiguration(32,76,1,2); content.addView(clock,new LinearLayout.LayoutParams(-1,dp(108)));
        clock.setContentDescription("Clock. Tap to open alarms"); clock.setOnClickListener(v -> launch(new Intent("android.intent.action.SHOW_ALARMS")));
        TextView greeting=text("YOUR SPACE. YOUR PACE.",11,muted); greeting.setLetterSpacing(.14f); content.addView(greeting); updateClock(); gap(content,28);
        LinearLayout cards=new LinearLayout(this); cards.setBaselineAligned(false);
        LinearLayout focus=card("◎  FOCUS"); TextView note=text(prefs.getString("focus","Good things\ntake time."),19,ink); note.setPadding(0,dp(16),0,dp(16)); focus.addView(note);
        focus.addView(text("Tap to edit",11,muted)); focus.setOnClickListener(v -> editFocus());
        LinearLayout today=card("◷  TODAY");
        for(int i=0;i<3;i++) { final int index=i; CheckBox check=new CheckBox(this); check.setText(prefs.getString("task"+i,new String[]{"Make something","Take a break","Plan tomorrow"}[i])); check.setTextSize(12); check.setTextColor(ink); check.setButtonTintList(android.content.res.ColorStateList.valueOf(accent)); check.setChecked(prefs.getBoolean("done"+i,false)); check.setOnCheckedChangeListener((b,checked) -> prefs.edit().putBoolean("done"+index,checked).apply()); check.setOnLongClickListener(v -> { editTask(index); return true; }); today.addView(check); }
        today.addView(text("Hold a task to edit",10,muted));
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-1,1); left.rightMargin=dp(10); cards.addView(focus,left); cards.addView(today,new LinearLayout.LayoutParams(0,-1,1)); content.addView(cards); gap(content,26);
        TextView favorites=text("FAVOURITES",11,muted); favorites.setLetterSpacing(.12f); content.addView(favorites); gap(content,10);
        ArrayList<App> favoritesList=new ArrayList<>(); for(String key:pinned) for(App app:apps) if(app.key.equals(key) && !isHidden(app)) favoritesList.add(app);
        if(favoritesList.isEmpty()) { TextView empty=text("Hold an app in the drawer to pin it here.",13,muted); empty.setPadding(0,dp(12),0,dp(18)); content.addView(empty); }
        else addGrid(content,favoritesList);
        gap(content,18); content.addView(button("⌕   Search apps  ·  All apps ↑",this::showDrawer),new LinearLayout.LayoutParams(-1,-2)); gap(content,20);
        LinearLayout dock=new LinearLayout(this);
        dockItem(dock,"☎","Phone",() -> launch(new Intent(Intent.ACTION_DIAL)));
        dockItem(dock,"☷","Messages",() -> launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)));
        dockItem(dock,"▣","Camera",() -> launch(new Intent("android.media.action.STILL_IMAGE_CAMERA")));
        dockItem(dock,"◎","Browser",() -> launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)));
        content.addView(dock);
        final float[] down={0}; scroll.setOnTouchListener((v,event) -> { if(event.getAction()==MotionEvent.ACTION_DOWN) down[0]=event.getY(); if(event.getAction()==MotionEvent.ACTION_UP && down[0]-event.getY()>dp(100) && !scroll.canScrollVertically(1)) { showDrawer(); return true; } return false; });
    }
    private LinearLayout card(String title) { LinearLayout c=vertical(); c.setPadding(dp(14),dp(16),dp(14),dp(16)); c.setBackground(surface(0xCC0A1825,0xFF224052,20)); c.addView(text(title,12,accent)); return c; }
    private void dockItem(LinearLayout row,String glyph,String label,Runnable action) { LinearLayout item=vertical(); item.setGravity(Gravity.CENTER); TextView icon=button(glyph,action); icon.setTextSize(24); item.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54))); TextView caption=text(label,11,muted); caption.setPadding(0,dp(7),0,0); item.addView(caption); row.addView(item,new LinearLayout.LayoutParams(0,-2,1)); }
    private void updateClock() { if(clock!=null) clock.setText(android.text.format.DateFormat.getTimeFormat(this).format(new Date())); if(date!=null) date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date())); }
    private void showDrawer() {
        drawer=true; shell.removeAllViews(); LinearLayout box=vertical(); box.setPadding(dp(24),dp(16),dp(24),0); shell.addView(box,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.addView(button("‹ Home",this::showHome)); TextView title=text("Your apps",24,ink); title.setPadding(dp(18),0,0,0); top.addView(title); box.addView(top); gap(box,18);
        search=new EditText(this); search.setSingleLine(true); search.setTextColor(ink); search.setHintTextColor(muted); search.setTextSize(16); search.setHint("Search installed apps"); search.setPadding(dp(16),dp(12),dp(16),dp(12)); search.setBackground(surface(0xEE0D202D,0xFF31586B,16)); box.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        count=text("",12,muted); count.setPadding(0,dp(12),0,dp(12)); box.addView(count);
        ScrollView scroller=new ScrollView(this); drawerList=vertical(); scroller.addView(drawerList); box.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int start,int n,int after){} public void onTextChanged(CharSequence s,int start,int before,int n){ filter(s.toString()); } public void afterTextChanged(Editable e){} });
        box.setFocusableInTouchMode(true); box.requestFocus(); filter("");
    }
    private void filter(String query) { if(!drawer || drawerList==null)return; ArrayList<App> filtered=new ArrayList<>(); for(App a:apps) if(!isHidden(a) && a.label.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) filtered.add(a); drawerList.removeAllViews(); count.setText(filtered.size()+" apps  ·  Hold to pin or manage"); if(filtered.isEmpty()) drawerList.addView(text("No matching apps",16,muted)); else addGrid(drawerList,filtered); }
    private void addGrid(LinearLayout parent,ArrayList<App> entries) {
        int columns=getResources().getConfiguration().screenWidthDp>=600 ? 6 : 4;
        for(int start=0;start<entries.size();start+=columns) { LinearLayout row=new LinearLayout(this); row.setPadding(0,dp(4),0,dp(12));
            for(int c=0;c<columns;c++) { final int pos=start+c; if(pos>=entries.size()) { row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1)); continue; }
                App app=entries.get(pos); LinearLayout tile=vertical(); tile.setGravity(Gravity.CENTER); tile.setPadding(dp(3),dp(4),dp(3),dp(4));
                ImageView icon=new ImageView(this); try { icon.setImageDrawable(app.info.loadIcon(getPackageManager())); } catch(Exception ignored) { icon.setImageDrawable(getPackageManager().getDefaultActivityIcon()); }
                icon.setPadding(dp(11),dp(11),dp(11),dp(11)); icon.setBackground(surface(0xCC0A1B28,0xFF315468,17)); tile.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
                TextView label=text(app.label,11,ink); label.setGravity(Gravity.CENTER); label.setMaxLines(2); label.setEllipsize(TextUtils.TruncateAt.END); label.setPadding(0,dp(7),0,0); tile.addView(label,new LinearLayout.LayoutParams(-1,dp(38)));
                tile.setContentDescription(app.label); tile.setOnClickListener(v -> launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))); tile.setOnLongClickListener(v -> { appMenu(app); return true; }); row.addView(tile,new LinearLayout.LayoutParams(0,-2,1));
            } parent.addView(row);
        }
    }
    private void loadApps() {
        final int generation=++loadGeneration;
        new Thread(() -> { ArrayList<App> found=new ArrayList<>(); Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo r:getPackageManager().queryIntentActivities(intent,0)) { if(r.activityInfo.packageName.equals(getPackageName()))continue; App a=new App(); a.info=r; a.label=r.loadLabel(getPackageManager()).toString(); a.component=new ComponentName(r.activityInfo.packageName,r.activityInfo.name); a.key=a.component.flattenToString(); found.add(a); }
            found.sort((a,b) -> a.label.compareToIgnoreCase(b.label)); runOnUiThread(() -> { if(isDestroyed() || generation!=loadGeneration)return; apps.clear(); apps.addAll(found); if(!prefs.contains("pinned")) { for(int i=0;i<Math.min(8,apps.size());i++)pinned.add(apps.get(i).key); savePins(); } if(privateUnlocked)showPrivate(); else if(drawer) filter(search.getText().toString()); else showHome(); });
        },"app-discovery").start();
    }
    private void savePins() { prefs.edit().putString("pinned",TextUtils.join("|",pinned)).apply(); }
    private void appMenu(App app) { if(isHidden(app)) { if(!privateUnlocked)return; privateDialog=new AlertDialog.Builder(this).setTitle(app.label).setItems(new String[]{"Turn off notifications…","Remove from secret folder","App info"},(d,which) -> { if(!privateUnlocked)return; if(which==0)launch(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,app.component.getPackageName())); if(which==1){hiddenPackages.remove(app.component.getPackageName()); saveHidden(); showPrivate();} if(which==2)launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.component.getPackageName()))); }).show(); privateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); return; } boolean isPinned=pinned.contains(app.key); new AlertDialog.Builder(this).setTitle(app.label).setItems(new String[]{isPinned?"Unpin from home":"Pin to home","App info"},(d,which) -> { if(which==0) { if(isPinned)pinned.remove(app.key); else if(pinned.size()<12)pinned.add(app.key); else { toast("You can pin up to 12 apps"); return; } savePins(); if(!drawer)showHome(); else toast(isPinned?"Unpinned":"Pinned to home"); } else launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.component.getPackageName()))); }).show(); }
    private void editFocus() { edit("Your focus",prefs.getString("focus","Good things take time."),120,value -> { prefs.edit().putString("focus",value).apply(); showHome(); }); }
    private void editTask(int i) { edit("Edit task",prefs.getString("task"+i,new String[]{"Make something","Take a break","Plan tomorrow"}[i]),40,value -> { prefs.edit().putString("task"+i,value).putBoolean("done"+i,false).apply(); showHome(); }); }
    private interface Save { void run(String value); }
    private void edit(String title,String current,int max,Save save) { EditText input=new EditText(this); input.setText(current); input.setTextColor(ink); input.setPadding(dp(24),dp(16),dp(24),dp(16)); input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)}); new AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w) -> { String value=input.getText().toString().trim(); if(!value.isEmpty())save.run(value); }).show(); }
    private void settings() { new AlertDialog.Builder(this).setTitle("Neon settings").setItems(new String[]{"Make Neon your home app","Accent colour","Edit focus note","Secret folder","About Neon"},(d,w) -> { if(w==0)launch(new Intent(Settings.ACTION_HOME_SETTINGS)); if(w==1)new AlertDialog.Builder(this).setTitle("Accent colour").setItems(new String[]{"Ice cyan","Electric violet","Mint"},(a,n) -> { accent=new int[]{0xFF70EEFA,0xFFB7A0FF,0xFF80F5BC}[n]; prefs.edit().putInt("accent",accent).apply(); showHome(); }).show(); if(w==2)editFocus(); if(w==3)unlockPrivate(); if(w==4)new AlertDialog.Builder(this).setTitle("Neon Home 0.1").setMessage("Your personal Android home screen.\n\nHold apps to pin them. Hold tasks to edit them. Tap the clock for alarms. Hold the NEON title for the secret folder.\n\nFocus notes, tasks and favourites stay on this device. No account or network access.\n\nTo switch back, open Android Settings → Apps → Default apps → Home app.").setPositiveButton("Done",null).show(); }).show(); }
    private boolean isHidden(App app) { return hiddenPackages.contains(app.component.getPackageName()); }
    private void saveHidden() { prefs.edit().putStringSet("hiddenPackages",new HashSet<>(hiddenPackages)).apply(); pinned.removeIf(key -> { ComponentName c=ComponentName.unflattenFromString(key); return c!=null && hiddenPackages.contains(c.getPackageName()); }); savePins(); }
    private void unlockPrivate() {
        KeyguardManager guard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(!guard.isDeviceSecure()) { new AlertDialog.Builder(this).setTitle("Set a phone screen lock first").setMessage("The secret folder uses your phone's PIN, pattern or password. Set one in Android security settings, then return here.").setPositiveButton("Security settings",(d,w) -> launch(new Intent(Settings.ACTION_SECURITY_SETTINGS))).setNegativeButton("Cancel",null).show(); return; }
        Intent confirm=guard.createConfirmDeviceCredentialIntent("Secret folder","Unlock with your phone's screen lock");
        if(confirm!=null)try { startActivityForResult(confirm,71); } catch(ActivityNotFoundException e) { toast("Screen-lock authentication is unavailable"); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) { super.onActivityResult(request,result,data); if(request==71 && result==RESULT_OK) { privateUnlocked=true; showPrivate(); } }
    private void showPrivate() {
        if(!privateUnlocked)return; hideKeyboard(); drawer=false; search=null; getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); shell.removeAllViews();
        ScrollView scroll=new ScrollView(this); LinearLayout box=vertical(); box.setPadding(dp(24),dp(24),dp(24),dp(24)); scroll.addView(box); shell.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        box.addView(button("Lock & return home",this::showHome)); gap(box,24); box.addView(text("Secret folder",30,accent)); gap(box,12);
        box.addView(text("Hidden from Neon's home screen and search. Locks whenever you leave Neon.",14,ink)); gap(box,12);
        box.addView(text("To stop alerts, hold each app below → Turn off notifications, then switch off notifications in Android settings. Hiding alone does not silence alerts. Apps remain visible in Android settings and other launchers.",13,muted)); gap(box,18);
        box.addView(button("Choose hidden apps",this::chooseHidden)); gap(box,24);
        ArrayList<App> secret=new ArrayList<>(); for(App a:apps)if(isHidden(a))secret.add(a); if(secret.isEmpty())box.addView(text("No hidden apps yet.",14,muted)); else addGrid(box,secret);
    }
    private void chooseHidden() {
        if(!privateUnlocked)return; ArrayList<App> choices=new ArrayList<>(); Set<String> seen=new HashSet<>(); for(App a:apps)if(seen.add(a.component.getPackageName()))choices.add(a);
        String[] labels=new String[choices.size()]; boolean[] checked=new boolean[choices.size()]; for(int i=0;i<labels.length;i++){labels[i]=choices.get(i).label;checked[i]=isHidden(choices.get(i));}
        privateDialog=new AlertDialog.Builder(this).setTitle("Choose hidden apps").setMultiChoiceItems(labels,checked,(d,i,value) -> checked[i]=value).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w) -> { if(!privateUnlocked)return; for(int i=0;i<checked.length;i++){String pkg=choices.get(i).component.getPackageName();if(checked[i])hiddenPackages.add(pkg);else hiddenPackages.remove(pkg);}saveHidden();showPrivate();}).show();
        privateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    private void launch(Intent intent) { try { ResolveInfo resolved=getPackageManager().resolveActivity(intent,0); if(resolved!=null && hiddenPackages.contains(resolved.activityInfo.packageName) && !privateUnlocked) { unlockPrivate(); return; } startActivity(intent); } catch(ActivityNotFoundException|SecurityException e) { toast("No available app for this action"); } }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }

    private class SpaceBackdrop extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        SpaceBackdrop() { super(MainActivity.this); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) { float w=getWidth(),h=getHeight(); canvas.drawColor(0xFF060D16);
            p.setShader(new RadialGradient(w*.96f,h*.17f,w*.8f,new int[]{0xFF183C50,0xFF060D16},null,Shader.TileMode.CLAMP)); canvas.drawRect(0,0,w,h,p); p.setShader(null);
            p.setColor(0xFF326077); canvas.drawCircle(w*.95f,h*.025f,w*.35f,p); p.setColor(0xFF09131F); canvas.drawCircle(w*.99f,h*.01f,w*.35f,p);
            Random random=new Random(42); for(int i=0;i<48;i++){p.setColor(Color.argb(35+random.nextInt(70),170,221,242)); canvas.drawCircle(random.nextFloat()*w,random.nextFloat()*h*.72f,dp(.6f),p);}
            for(int layer=0;layer<4;layer++){Path path=new Path();float y=h*(.64f+layer*.09f);path.moveTo(0,h);path.lineTo(0,y);for(int x=0;x<=12;x++)path.lineTo(w*x/12f,y-(float)Math.sin(x*1.8+layer)*dp(18)-random.nextInt(dp(26)));path.lineTo(w,h);path.close();p.setColor(new int[]{0xFF0C2130,0xFF0A1B29,0xFF081521,0xFF060F19}[layer]);canvas.drawPath(path,p);}
        }
    }
}


