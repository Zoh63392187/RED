<html>
<head>
<link href="PLUGINS_ROOT/org.robotframework.ide.eclipse.main.plugin.doc.user/help/style.css" rel="stylesheet" type="text/css"/>
</head>
<body>
<a href="RED/../../../../../help/index.html">RED - Robot Editor User Guide</a> &gt; <a href="RED/../../../../../help/user_guide/user_guide.html">User guide</a> &gt; <a href="RED/../../../../../help/user_guide/launching.html">Launching Tests</a> &gt; <a href="RED/../../../../../help/user_guide/launching/debug.html">Debugging Robot</a> &gt; 
	<h2>User interface</h2>
<p>Debug perspective looks as follows:
	</p>
<img src="images/debug_perspective.png"/>
<p>Major parts of this perspective are:
	</p>
<ul>
<li><b>Controller toolbar</b> from which user can perform <a href="hitting_a_breakpoint.html">stepping</a> 
		as well as resuming, suspending, terminating and disconnecting from running tests (see 
		<a href="../exec_control.html">Controlling execution</a>). It is also possible to deactivate all the breakpoints
		globally using <b>Skip All Breakpoints</b> button.
		</li>
<li><b>Execution</b> and <b>Message Log</b> views described <a href="../ui_elements.html">here</a>.
		</li>
<li><b>Debug</b> view where currently launched session is visible. When <a href="hitting_a_breakpoint.html">suspended</a>
		there is a stack presented showing current path to root in suites execution tree.
		</li>
<li><b>Variables</b> view presenting variables bounded with selected stack frame from Debug view. It is also
		possible to edit values of those variables (see <a href="hitting_a_breakpoint.html">Changing variables</a>).
		</li>
<li><b>Breakpoints</b> view gathering all the breakpoints defined in users workspace. It allows to edit
		as well as enable/disable the breakpoints - see <a href="breakpoints.html">Breakpoints</a>.
		</li>
<li><b>editor area</b>; when execution suspends and user opens stack frames in <b>Debug</b> view the source code 
		related to selected frame is shown in editor. There is a current line highlighted and also other execution
		related information (see <a href="hitting_a_breakpoint.html">Hitting a breakpoint</a>).
		</li>
</ul>
<br/>
</body>
</html>