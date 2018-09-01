extern crate ir;
extern crate serde_json;

use std::collections::{BTreeMap, HashMap, HashSet, VecDeque};

use ir::{Edge, Gate, Node, Type};

pub fn loop_free(graph: &HashMap<String, Gate>, gate: &Gate) -> bool {
    let mut seen = HashSet::new();
    let mut grey = HashSet::new();

    fn contains_loop(
        gate: &Gate,
        seen: &mut HashSet<Node>,
        grey: &mut HashSet<Node>,
        node: Node,
    ) -> bool {
        if grey.contains(&node) {
            return false;
        }

        grey.insert(node.clone());
        if let Some(edges) = gate.edges.get(&node.name) {
            for edge in edges {
                let (ref node_name, _) = edge.sink;
                let node = gate
                    .nodes
                    .get(node_name)
                    .expect("edge leading to nonexistant node");
                if seen.contains(&node) {
                    continue;
                }
                if !contains_loop(gate, seen, grey, node.clone()) {
                    return false;
                }
            }
        }

        grey.remove(&node);
        seen.insert(node);

        true
    }

    if !gate
        .inputs
        .iter()
        .filter_map(|name| gate.nodes.get(name).cloned())
        .all(|node| contains_loop(gate, &mut seen, &mut grey, node.clone()))
    {
        return false;
    }

    if !gate
        .nodes
        .iter()
        .map(|(_, node)| node)
        .filter_map(|node| match node.type_ {
            Type::Internal(ref x) => Some(x),
            _ => None,
        }).filter(|gate_name| gate_name.as_str() != "nand")
        .map(|gate_name| graph.get(gate_name).expect("could not find gate"))
        .all(|gate| loop_free(graph, gate))
    {
        return false;
    }

    true
}

// #[test]
// fn test_loop_free() {
//     let gate: Gate = serde_json::from_str(include_str!("no_loops_test.json")).unwrap();
//     assert!(loop_free(&Default::default(), &gate));

//     let gate: Gate = serde_json::from_str(include_str!("loops_test.json")).unwrap();
//     assert!(!loop_free(&Default::default(), &gate));
// }

#[derive(Debug, Default, Hash, PartialEq, Eq, Clone)]
struct GateInput(BTreeMap<String, GateOutput>);

#[derive(Debug, Default, Hash, PartialEq, Eq, Clone, Copy)]
struct GateOutput(bool);

#[derive(Debug, Clone)]
struct TruthTable(HashMap<GateInput, Option<GateOutput>>);

fn truth_table(
    graph: &HashMap<String, Gate>,
    precalculated: &mut HashMap<String, TruthTable>,
    gate_name: &str,
) -> TruthTable {
    if !precalculated.contains_key(gate_name) {
        if gate_name == "nand" {
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
                output.insert(GateInput(input), Some(GateOutput(*o)));
            }
            let output = TruthTable(output);

            precalculated.insert("nand".into(), output.clone());

            return output;
        }

        let gate = graph.get(gate_name).expect("Gate does not exist");
        let input_possibilities = (0..)
            .map(|num| {
                gate.inputs
                    .iter()
                    .enumerate()
                    .map(|(bit, name)| {
                        let value = num & (1 << bit) != 0;
                        (name.clone(), GateOutput(value))
                    }).collect::<BTreeMap<_, _>>()
            }).take(1 << gate.inputs.len())
            .collect::<Vec<_>>();

        let mut output = HashMap::new();

        for input in input_possibilities {
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
                                let values_needed = if gate_type == "nand" {
                                    2
                                } else {
                                    graph.get(&gate_type).expect("undefined gate").inputs.len()
                                };
                                if currently_calculated.0.len() == values_needed {
                                    // we've found all the inputs to this gate, lets move on
                                    let TruthTable(truth_table_for_node) =
                                        truth_table(graph, precalculated, &gate_type);

                                    if let Some(Some(GateOutput(node_output))) = truth_table_for_node
                                        .get(&currently_calculated.clone())
                                    {
                                        queue.push_back((node_name.clone(), GateOutput(*node_output)));
                                    }
                                }
                            }
                        }
                    }
                }

                break 'output None;
            };

            output.insert(GateInput(input), gate_output);
        }

        precalculated.insert(gate_name.into(), TruthTable(output));
    }

    precalculated.get(gate_name).cloned().unwrap()
}

#[test]
fn test_truth_table() {
    let graph = serde_json::from_str(include_str!("latch.json")).unwrap();
    let mut lookups = Default::default();
    let table = truth_table(&graph, &mut lookups, "latch");

    panic!("{:#?}", table);
}
