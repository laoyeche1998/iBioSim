package analysis.dynamicsim.hierarchical.methods;

import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.HighamHall54Integrator;

import analysis.dynamicsim.hierarchical.simulator.HierarchicalSimulation;
import analysis.dynamicsim.hierarchical.states.ModelState;
import analysis.dynamicsim.hierarchical.util.HierarchicalUtilities;
import analysis.dynamicsim.hierarchical.util.comp.HierarchicalEventComparator;
import analysis.dynamicsim.hierarchical.util.io.HierarchicalWriter;
import analysis.dynamicsim.hierarchical.util.math.EventNode;
import analysis.dynamicsim.hierarchical.util.math.ReactionNode;
import analysis.dynamicsim.hierarchical.util.setup.ModelSetup;

public final class HierarchicalODERKSimulator extends HierarchicalSimulation
{
	private boolean						isSingleStep;
	private double						printTime	= 0;
	private HighamHall54Integrator		odecalc;
	private final boolean				print;
	private long						randomSeed;
	private double						relativeError, absoluteError;

	private double[]					state;
	private List<ReactionNode>			reactionList;
	private List<EventNode>				eventList;
	private PriorityQueue<EventNode>	triggeredEventList;

	public HierarchicalODERKSimulator(HierarchicalSimulation sim, ModelState topModel, long randomSeed) throws IOException, XMLStreamException
	{
		super(sim);
		setTopmodel(topModel);
		this.relativeError = 1e-6;
		this.absoluteError = 1e-9;
		this.print = false;
		this.randomSeed = randomSeed;
		this.odecalc = new HighamHall54Integrator(getMinTimeStep(), getMaxTimeStep(), absoluteError, relativeError);
		this.isInitialized = false;
		this.isSingleStep = true;
		this.printTime = Double.POSITIVE_INFINITY;
	}

	public HierarchicalODERKSimulator(String SBMLFileName, String rootDirectory, double timeLimit) throws IOException, XMLStreamException
	{

		this(SBMLFileName, rootDirectory, rootDirectory, 0, timeLimit, Double.POSITIVE_INFINITY, 0, null, Double.POSITIVE_INFINITY, 0, null, null, 1, 1e-6, 1e-9, "amount", "none", false);
		isInitialized = false;
		isSingleStep = true;
		this.printTime = Double.POSITIVE_INFINITY;
	}

	public HierarchicalODERKSimulator(String SBMLFileName, String rootDirectory, String outputDirectory, int runs, double timeLimit, double maxTimeStep, long randomSeed, JProgressBar progress, double printInterval, double stoichAmpValue, JFrame running, String[] interestingSpecies, int numSteps,
			double relError, double absError, String quantityType, String abstraction) throws IOException, XMLStreamException
	{

		this(SBMLFileName, rootDirectory, outputDirectory, runs, timeLimit, maxTimeStep, randomSeed, progress, printInterval, stoichAmpValue, running, interestingSpecies, numSteps, relError, absError, quantityType, abstraction, true);
		this.isInitialized = false;
		this.printTime = 0;
		this.randomSeed = randomSeed;
		this.isSingleStep = false;

	}

	public HierarchicalODERKSimulator(String SBMLFileName, String rootDirectory, String outputDirectory, int runs, double timeLimit, double maxTimeStep, long randomSeed, JProgressBar progress, double printInterval, double stoichAmpValue, JFrame running, String[] interestingSpecies, int numSteps,
			double relError, double absError, String quantityType, String abstraction, boolean print) throws IOException, XMLStreamException
	{

		super(SBMLFileName, rootDirectory, outputDirectory, runs, timeLimit, maxTimeStep, 0.0, progress, printInterval, stoichAmpValue, running, interestingSpecies, quantityType, abstraction, SimType.HODE);
		this.randomSeed = randomSeed;
		this.relativeError = relError;
		this.absoluteError = absError;
		this.print = print;
		this.printTime = 0;
		this.isSingleStep = false;
		this.absoluteError = absoluteError == 0 ? 1e-12 : absoluteError;
		this.relativeError = absoluteError == 0 ? 1e-9 : relativeError;

		if (numSteps > 0)
		{
			setPrintInterval(getTimeLimit() / numSteps);
		}

		odecalc = new HighamHall54Integrator(getMinTimeStep(), getMaxTimeStep(), absoluteError, relativeError);
		isInitialized = false;

	}

	@Override
	public void cancel()
	{
		setCancelFlag(true);

	}

	@Override
	public void clear()
	{

	}

	@Override
	public void initialize(long randomSeed, int runNumber) throws IOException, XMLStreamException
	{
		if (!isInitialized)
		{
			setCurrentTime(0);
			ModelSetup.setupModels(this, false);
			eventList = getEventList();
			variableList = getVariableList();
			reactionList = getReactionList();

			HierarchicalUtilities.computeFixedPoint(variableList, reactionList);

			if (!eventList.isEmpty())
			{
				HierarchicalEventHandler handler = new HierarchicalEventHandler();
				HierarchicalTriggeredEventHandler triggeredHandler = new HierarchicalTriggeredEventHandler();
				odecalc.addEventHandler(handler, getPrintInterval(), 1e-20, 10000);
				odecalc.addEventHandler(triggeredHandler, getPrintInterval(), 1e-20, 10000);
				triggeredEventList = new PriorityQueue<EventNode>(new HierarchicalEventComparator());

				HierarchicalUtilities.triggerAndFireEvents(eventList, triggeredEventList, currentTime.getValue());

			}
			state = getArrayState(variableList);

			initStateCopy = state.clone();
			if (!isSingleStep)
			{
				setupForOutput(runNumber);

				HierarchicalWriter.setupVariableFromTSD(getBufferedTSDWriter(), getTopmodel(), getSubmodels(), getInterestingSpecies());
			}
			isInitialized = true;
		}

	}

	@Override
	public void simulate()
	{
		if (!isInitialized)
		{
			try
			{
				this.initialize(randomSeed, 1);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (XMLStreamException e)
			{
				e.printStackTrace();
			}
		}

		double nextEndTime = 0;

		DifferentialEquations de = new DifferentialEquations();

		while (currentTime.getValue() < getTimeLimit() && !isCancelFlag() && !isConstraintFlag())
		{
			nextEndTime = currentTime.getValue() + getMaxTimeStep();
			if (nextEndTime > printTime)
			{
				nextEndTime = printTime;
			}

			if (nextEndTime > getTimeLimit())
			{
				nextEndTime = getTimeLimit();
			}

			if (state.length > 0)
			{
				try
				{

					odecalc.integrate(de, currentTime.getValue(), state, nextEndTime, state);
					HierarchicalUtilities.computeAssignmentRules(state, variableList);

				}
				catch (Exception e)
				{
					setCurrentTime(nextEndTime);
				}
			}
			else
			{
				setCurrentTime(nextEndTime);
			}
			if (!isSingleStep)
			{
				while (currentTime.getValue() >= printTime && printTime <= getTimeLimit())
				{
					try
					{
						HierarchicalWriter.printToTSD(getBufferedTSDWriter(), getTopmodel(), getSubmodels(), getInterestingSpecies(), getPrintConcentrationSpecies(), printTime);
						getBufferedTSDWriter().write(",\n");
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					printTime = printTime + getPrintInterval();

					if (getRunning() != null)
					{
						getRunning().setTitle("Progress (" + (int) ((getCurrentTime().getValue() / getTimeLimit()) * 100.0) + "%)");
					}
				}
			}
		}
		if (!isSingleStep)
		{
			try
			{
				getBufferedTSDWriter().write(')');
				getBufferedTSDWriter().flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setupForNewRun(int newRun)
	{
		if (isInitialized)
		{
			state = initStateCopy.clone();
		}
	}

	@Override
	public void printStatisticsTSD()
	{
		// TODO Auto-generated method stub

	}

	private void updateVariables(double[] y)
	{
		for (int i = 0; i < y.length; i++)
		{
			variableList.get(i).setValue(y[i]);
		}
	}

	public class HierarchicalEventHandler implements EventHandler
	{
		private double	value	= -1;

		@Override
		public void init(double t0, double[] y0, double t)
		{

		}

		@Override
		public double g(double t, double[] y)
		{
			double returnValue = -value;

			currentTime.setValue(t);
			for (int i = 0; i < y.length; i++)
			{
				variableList.get(i).setValue(y[i]);
			}

			for (EventNode event : eventList)
			{
				if (event.isTriggeredAtTime(t))
				{
					returnValue = value;
				}
			}
			return returnValue;
		}

		@Override
		public Action eventOccurred(double t, double[] y, boolean increasing)
		{
			value = -value;

			currentTime.setValue(t);

			for (int i = 0; i < y.length; i++)
			{
				variableList.get(i).setValue(y[i]);
			}

			HierarchicalUtilities.computeAssignmentRules(state, variableList);
			HierarchicalUtilities.triggerAndFireEvents(eventList, triggeredEventList, t);
			HierarchicalUtilities.computeAssignmentRules(y, variableList);

			return EventHandler.Action.STOP;
		}

		@Override
		public void resetState(double t, double[] y)
		{
			// TODO Auto-generated method stub

		}

	}

	public class HierarchicalTriggeredEventHandler implements EventHandler
	{
		private double	value	= -1;

		@Override
		public void init(double t0, double[] y0, double t)
		{

		}

		@Override
		public double g(double t, double[] y)
		{
			currentTime.setValue(t);
			if (!triggeredEventList.isEmpty())
			{
				if (triggeredEventList.peek().getFireTime() <= t)
				{
					return value;
				}
			}
			return -value;
		}

		@Override
		public Action eventOccurred(double t, double[] y, boolean increasing)
		{
			value = -value;

			currentTime.setValue(t);

			for (int i = 0; i < y.length; i++)
			{
				variableList.get(i).setValue(y[i]);
			}

			HierarchicalUtilities.computeAssignmentRules(state, variableList);
			HierarchicalUtilities.triggerAndFireEvents(eventList, triggeredEventList, t);
			HierarchicalUtilities.computeAssignmentRules(y, variableList);

			return EventHandler.Action.STOP;
		}

		@Override
		public void resetState(double t, double[] y)
		{
			// TODO Auto-generated method stub

		}

	}

	public class DifferentialEquations implements FirstOrderDifferentialEquations
	{

		@Override
		public int getDimension()
		{
			return variableList.size();
		}

		@Override
		public void computeDerivatives(double t, double[] y, double[] yDot) throws MaxCountExceededException, DimensionMismatchException
		{
			setCurrentTime(t);
			// Copy values
			updateVariables(y);
			HierarchicalUtilities.computeAssignmentRules(y, variableList);
			HierarchicalUtilities.computeReactionPropensities(reactionList);
			//
			for (int i = 0; i < yDot.length; i++)
			{
				yDot[i] = variableList.get(i).computeRateOfChange(t);
			}
		}
	}

}
