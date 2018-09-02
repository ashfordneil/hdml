use std::collections::{BTreeMap, HashMap, HashSet, VecDeque};

use ir::{Edge, Gate, Type};

use serde_json;
use itertools::Itertools;

#[derive(Debug, Default, Hash, PartialEq, Eq, Clone)]
struct GateInput(BTreeMap<String, GateOutput>);

#[derive(Debug, Default, Hash, PartialEq, Eq, Clone, Copy)]
struct GateOutput(bool);

#[derive(Debug, Clone)]
pub struct TruthTable(HashMap<GateInput, GateOutput>);

fn all_inputs(gate: &Gate) -> Vec<GateInput> {
    (0..)
        .map(|num| {
            gate.inputs
                .iter()
                .enumerate()
                .map(|(bit, name)| {
                    let value = num & (1 << bit) != 0;
                    (name.clone(), GateOutput(value))
                }).collect::<BTreeMap<_, _>>()
        }).take(1 << gate.inputs.len())
        .map(GateInput)
        .collect::<Vec<_>>()
}

fn nand_truth_table() -> TruthTable {
    let mut output = HashMap::new();
    for (x, y, o) in &[
        (false, false, true),
        (false, true, true),
        (true, false, true),
        (true, true, false),
    ] {
        let input = (&[("x", x), ("y", y)])
            .iter()
            .map(|(x, &y)| (x.to_string(), GateOutput(y)))
            .collect();
        output.insert(GateInput(input), GateOutput(*o));
    }
    TruthTable(output)
}

fn calculate_value(
    graph: &HashMap<String, Gate>,
    precalculated: &mut HashMap<String, TruthTable>,
    gate_type: &str,
    inputs: &GateInput,
) -> Option<GateOutput> {
    let TruthTable(truth_table) = truth_table(graph, precalculated, gate_type);
    let all_inputs = if gate_type == "nand" {
        nand_truth_table().0.into_iter().map(|(key, _val)| key).collect()
    } else {
        let gate = graph.get(gate_type).unwrap();
        all_inputs(gate)
    };
    let GateInput(current_inputs) = inputs;

    let mut possible_outputs = all_inputs
        .into_iter()
        .filter(|GateInput(input)| {
            current_inputs
                .iter()
                .all(|(key, val)| input.get(key) == Some(val))
        }).map(|input| truth_table.get(&input).cloned().unwrap());

    let init = Some(
        possible_outputs
            .next()
            .expect("Invalid series of gate inputs"),
    );
    possible_outputs.fold(init, |init, next| match (init, next) {
        (Some(GateOutput(x)), GateOutput(y)) if x == y => Some(GateOutput(x)),
        _ => None,
    })
}

pub fn truth_table<'a>(
    graph: &'a HashMap<String, Gate>,
    precalculated: &'a mut HashMap<String, TruthTable>,
    gate_name: &'a str,
) -> &'a TruthTable {
    if !precalculated.contains_key(gate_name) {
        if gate_name == "nand" {
            precalculated.insert("nand".into(), nand_truth_table());
            return precalculated.get("nand").unwrap();
        }

        let gate = graph.get(gate_name).expect("Gate does not exist");
        let input_possibilities = all_inputs(&gate);

        let mut output = HashMap::new();

        for GateInput(input) in input_possibilities {
            // map from node name to state of inputs to node
            // state of inputs to node is map from incomming edge name to value on that edge (if
            // calculated)
            let mut states: HashMap<String, GateInput> = Default::default();

            // map of node name to recently computed value of node
            let mut queue: VecDeque<(String, GateOutput)> =
                input.iter().map(|(x, &y)| (x.clone(), y)).collect();
            queue.push_back(("1".into(), GateOutput(true)));
            queue.push_back(("0".into(), GateOutput(false)));

            let gate_output = 'output: loop {
                while let Some((node_name, GateOutput(value))) = queue.pop_front() {
                    let edges = match gate.edges.get(node_name.as_str()) {
                        Some(x) => x,
                        None => continue,
                    };

                    for edge in edges.iter() {
                        let Edge {
                            source: _,
                            sink: (node_name, entry_name),
                        } = edge;

                        states
                            .entry(node_name.clone())
                            .or_insert_with(|| Default::default())
                            .0
                            .insert(entry_name.clone(), GateOutput(value));

                        let currently_calculated = states.get(node_name.as_str()).unwrap();
                        match gate.nodes.get(node_name.as_str()).cloned().unwrap().type_ {
                            Type::Input => panic!("Wire leading into an input"),
                            Type::Output => {
                                break 'output Some(GateOutput(value));
                            }
                            Type::Internal(gate_type) => {
                                if let Some(node_output) = calculate_value(
                                    graph,
                                    precalculated,
                                    gate_type.as_str(),
                                    currently_calculated,
                                ) {
                                    queue.push_back((node_name.clone(), node_output));
                                }
                            }
                        }
                    }
                }

                break 'output None;
            };

            if let Some(gate_output) = gate_output {
                output.insert(GateInput(input), gate_output);
            }
        }

        precalculated.insert(gate_name.into(), TruthTable(output));
    }

    precalculated.get(gate_name).unwrap()
}

pub fn display_truth_table(gate: &Gate, truth_table: &TruthTable) -> String {
    let TruthTable(truth_table) = truth_table;
    let all_inputs = all_inputs(gate);

    if all_inputs.is_empty() {
        return "".into();
    }

    let headers = all_inputs[0].0.iter().map(|(name, _)| name.as_str()).intersperse(",").collect();
    return headers;
}
