
package ie.ucd.clops.runtime.automaton;

/*
 * The implementation slighty follows the reg-exp NDA implementation at
 * http://swtch.com/~rsc/regexp/
 */

import ie.ucd.clops.runtime.automaton.exception.EmptyAlternativeException;
import ie.ucd.clops.runtime.automaton.exception.EmptyFormatException;
import ie.ucd.clops.runtime.automaton.exception.LeftOpenBracketException;
import ie.ucd.clops.runtime.automaton.exception.OpenPlusException;
import ie.ucd.clops.runtime.automaton.exception.OpenQuestionException;
import ie.ucd.clops.runtime.automaton.exception.OpenStarException;
import ie.ucd.clops.runtime.automaton.exception.RightOpenBracketException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Set;
import java.util.IdentityHashMap;



/**
 * Represents a regular expression (command line format) as a finite-state automaton and enables traversing
 * the automaton with tokens of the regular expression (options).
 *
 * @param <T> TODO
 *
 * @author Viliam Holub
 * @author Mikolas Janota
 */
public class Automaton<T> {

	/*
	 * Automaton fields
	 */
	/** Starting state of the automaton. */
	State<T> /*@ non_null @*/ start_state;


	/** Automaton state.
	 */
	public class AutomatonRun {
		/** Indication of error state.
		 * If this variable is set, we are in the error state. */
		boolean error;
		/** List of active states.
		 * The list is updated every step. */
		IdentityHashMap<State<T>,Integer> /*@ non_null @*/ actives;
		/** Backup of active states. Used for backtracking. */
		Stack<IdentityHashMap<State<T>,Integer>> archive;

		AutomatonRun() {
			error = false;
			actives = new IdentityHashMap<State<T>,Integer>();
			archive = new Stack<IdentityHashMap<State<T>,Integer>>();
			addSuccessors( start_state, actives);
		}


		/** Adds MATCH and END successors of a state.
		 * To avoid duplicates in the output list (and avoid cycling as well),
		 * we update each state with step index and process only those, which
		 * state index is less than the current one. Uses recursion to
		 * transitively explore the graph.
		 * @param state state to add or follow
		 * @param successors output list of states
		 */
		private void addSuccessors2(
				/*@ non_null @*/ State<T> state,
				/*@ non_null @*/ IdentityHashMap<State<T>,Integer> nexts) {
			if (state == null || nexts.containsKey( state))
				return;
			nexts.put( state, null);
			//state.state_index = step_index; //XXX
			if (state.type == StateType.SPLIT) {
				addSuccessors2( state.next1, nexts);
				addSuccessors2( state.next2, nexts);
			} else {
				nexts.put( state, null);
			}
		}


		/** Adds successors of s of the type MATCH or END to the list.
		 * Type of the state s must by MATCH or END.
		 * @param state state to follow
		 * @param next output successor states
		 */
		private void addSuccessors(
				/*@ non_null @*/ State<T> state,
				/*@ non_null @*/ IdentityHashMap<State<T>,Integer> nexts) {
			addSuccessors2( state.next1, nexts);
			addSuccessors2( state.next2, nexts);
		}


		/** Follows specified transitions in the automaton and returns MATCH and END successor states.
		 * @param state state to follow
		 * @param transition_labels a collection of transition labels to process
		 * @param actives list of currently active states
		 * @param nexts output list of successor states
		 */
		private void follow(
				/*@ non_null @*/ State<T> state,
				/*@ non_null @*/ Collection<T> transition_labels,
				/*@ non_null @*/ IdentityHashMap<State<T>,Integer> nexts) {

			// Type of the state must be ready to match.
			if (state.type != StateType.MATCH)
				return;
			// Follow all the transition labels.
			for (T transition_label:transition_labels) {
				if (state.match.equals( transition_label)) {
					// We have a match, add succesors
					addSuccessors( state, nexts);
				}
			}
		}


		/** Apply next step in automaton.
		 * If possible (i.e. matched), we follow all the transitions for all the active states.
		 * @param t a collection of transition labels to process
		 * @return true iff we could follow at least one of the transition labels
		 */
		public boolean nextStep( /*@ non_null @*/ Collection<T> t) {
			// Do not continue if we are in an error state
			if (error)
				return false;

			// Process next step, store states
			archive.push( actives);
			IdentityHashMap<State<T>,Integer> nexts = new IdentityHashMap<State<T>,Integer>();
			for (State<T> s:actives.keySet())
				follow( s, t, nexts);
			actives = nexts;

			// If the resulting list of states is empty, we are in an error
			// state
			error = actives.isEmpty();

			return !error;
		}


		/** Apply next step in automaton.
		 * @param transition a transition label to process
		 * @return true iff it was possible to follow the given transition 
		 */
		public boolean nextStep( /*@ non_null @*/ T transition) {
			Collection<T> trans = new LinkedList<T>();
			trans.add( transition);
			return nextStep( trans);
		}


		/** Determines whether the automaton is in an accepting state.
		 * Automaton is accepting if at least one of the current automaton
		 * states is accepting.
		 *
		 * @return true iff the automaton is in an accepting state
		 */
		/*@pure*/public boolean isAccepting() {
			for (State<T> s:actives.keySet()) {
				if (s.isAccepting())
					return true;
			}
			return false;
		}


		/** Determines whether the automaton is in an error state.
		 * @return true iff the automaton is in an error state
		 */
		/*@pure*/public boolean inErrorState() {
			return error;
		}

		
		/** Computes a list of transitions that lead from the current state to 
		 * a state that is not an error state.
		 * @return list of available trantions
		 */
		//@ ensures \fresh(\result);
		/*@pure*/public /*@non_null*/List<T> availableTransitions() {
			List<T> transitions = new LinkedList<T>();
			for (State<T> state : actives.keySet())
				if (state.type == StateType.MATCH)
					transitions.add( state.match);
			return transitions;
		}

		
		/** Computes a set of available transitions.
		 * @see ie.ucd.clops.runtime.automaton.Automaton#availableTransitions()
		 * @return set of available transitions
		 */
		//@ ensures \fresh(\result);
		/*@pure*/public /*@non_null*/HashSet<T> availableTransitionsUnique() {
		    return new HashSet<T>( availableTransitions());
		}


		/** Undo the last {@code nextStep()} call. */
		public void previousStep() {
			actives = archive.pop();
		}


		/** Returns a set of active states.
		 */
		public Set<State<T>> activeStates() {
			return actives.keySet();
		}
	}


	/*
	 * Code
	 */

	/** Creates an automaton representation of a given command line format.
         * @param tokens a regular expression represented as a set of tokens
         * @see ie.ucd.clops.runtime.automaton.Tokenizer
	 */
	//@ tokens.size() != 0;
	public Automaton( /*@ non_null @*/ List<Token<T>> tokens)
			throws RightOpenBracketException, LeftOpenBracketException,
			OpenQuestionException, EmptyAlternativeException,
			OpenStarException, OpenPlusException, EmptyFormatException {

		build( tokens);
	}

	/**
	 * Context class used for format parsing and automation building.
	 */
	private class Context {
		int alternatives; // Level of alternatives "|"
		int atoms; // Number of atoms (options to concatenate)
	}

	/**
	 * Builds an automaton from a given  list of tokens.
	 */
	//@ tokens.size() != 0;
	private void build( /*@ non_null @*/ List<Token<T>> tokens)
			throws RightOpenBracketException, LeftOpenBracketException,
			OpenQuestionException, EmptyAlternativeException,
			OpenStarException, OpenPlusException, EmptyFormatException {
		// Stack of contexts, each context represents nested ()
		Stack<Context> ctxs = new Stack<Context>();
		// Fragments of automaton
		Stack<Fragment<T>> fragments = new Stack<Fragment<T>>();
		// Current context
		Context ctx = new Context();

		for (Token<T> t:tokens) switch (t.type) {
			case MATCH:
				// If there are two atom fragments on stack, concatenate
				if (ctx.atoms > 1) {
					Fragment<T> f = fragments.pop();
					fragments.peek().concatenate( f);
					ctx.atoms--;
				}
				// Create fragment that matches the given string
				// Push fragment into stack
				fragments.push( new Fragment<T>( new State<T>( StateType.MATCH, t.match)));
				ctx.atoms++;
				break;
			case LEFT:
				// Concatenate atoms on stack
				if (ctx.atoms > 1) {
					Fragment<T> f = fragments.pop();
					fragments.peek().concatenate( f);
					ctx.atoms--;
				}
				// Save context and create a new one
				ctxs.push( ctx);
				ctx = new Context();
				break;
			case RIGHT:
				// Test if right bracket without left one
				if (ctxs.size() == 0)
					throw new RightOpenBracketException();
				
				// If there are no atoms, syntax error -- ok if alternatives != 0
				if (ctx.atoms == 0) {
					throw new RightOpenBracketException();
				}
				// Concatenate atoms on stack
				if (ctx.atoms > 1) {
					Fragment<T> f = fragments.pop();
					fragments.peek().concatenate( f);
					ctx.atoms--;
				}
				// Create alternatives from fragments on stack
				if (ctx.alternatives > 0) {
					if (fragments.size() == 1)
						throw new EmptyAlternativeException();
					fragments.push( Fragment.alternative(
						fragments.pop(), fragments.pop()));
				}
				// Recover old context
				ctx = ctxs.pop();
				ctx.atoms++;
				break;
			case OR:
				if (ctx.atoms == 0 && ctx.alternatives != 0) {
					// Use of multiple alternatives ||
					// Ignore this one
					break;
				}
				if (ctx.atoms == 0 && ctx.alternatives == 0) {
					// If there are no fragments, raise error
					throw new EmptyAlternativeException();
				}
				
				// Concatenate fragments on stack
				while (ctx.atoms>1) {
					// Concatenate two fragments
					Fragment<T> f = fragments.pop();
					fragments.peek().concatenate( f);
					ctx.atoms--;
				}
				// Continue to make alternatives...
				if (ctx.alternatives > 0) {
					fragments.push( Fragment.alternative(
						fragments.pop(), fragments.pop()));
				}
				// Set status of alternatives
				ctx.alternatives = 1;
				ctx.atoms = 0;
				break;
			case PLUS:
				// If there are no atom fragments, raise error
				if (ctx.atoms == 0) throw new OpenPlusException();
				// Apply operator to the last fragment
				fragments.push( Fragment.apply_operator( TokenType.PLUS, fragments.pop()));
				break;
			case STAR:
				// If there are no atom fragments, raise error
				if (ctx.atoms == 0) throw new OpenStarException();
				// Apply operator to the last fragment
				fragments.push( Fragment.apply_operator( TokenType.STAR, fragments.pop()));
				break;
			case QUESTION:
				// If there are no atom fragments, raise error
				if (ctx.atoms == 0) throw new OpenQuestionException();
				// Apply operator to the last fragment
				fragments.push( Fragment.apply_operator( TokenType.QUESTION, fragments.pop()));
				break;
		}

		// Report error if there are unclosed brackets
		if (ctxs.size() != 0)
			throw new LeftOpenBracketException();
		
		// If the stack is empty, comply
		if (fragments.size() == 0) {
			throw new EmptyFormatException();
		}
		// Concatenate any residual atoms
		if (ctx.atoms > 1) {
			Fragment<T> f = fragments.pop();
			fragments.peek().concatenate( f);
		}
		// If there is unclosed alternative, close it
		if (ctx.alternatives != 0) {
			if (fragments.size() == 1)
				throw new EmptyAlternativeException();
			fragments.push( Fragment.alternative(
					fragments.pop(), fragments.pop()));
		}
		// Create final fragment
		State<T> s = new State<T>( StateType.END, null, null, null);
		Fragment<T> fin = fragments.pop();
		fin.assignNext( s);

		assert fragments.isEmpty();

		// Write start state
		start_state = new State<T>( StateType.SPLIT, null, fin.start, null);
	}


	/** Returns followable NDA state.
	 */
	public AutomatonRun run() {
		return new AutomatonRun();
	}


	/** Returns initial state.
	 */
	public State<T> getInitialState() {
		return start_state;
	}

		
	/** Computes a list of transitions that lead from the state specified.
	 *
	 * @param state root state
	 * @return list of available trantions
	 */
	/*@pure*/public /*@non_null*/List<T> getOutgoing( State<T> state) {
		List<T> transitions = new LinkedList<T>();
		if (state.type == StateType.MATCH)
			transitions.add( state.match);
		return transitions;
	}
}
