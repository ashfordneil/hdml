//! The IR of HDML.
extern crate serde;
#[macro_use]
extern crate serde_derive;

use std::collections::{HashSet, HashMap};
<<<<<<< Updated upstream
=======

pub struct Graph {
    gates: HashMap<String, Gate>,
}
>>>>>>> Stashed changes

/// A gate
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Gate {
<<<<<<< Updated upstream
    pub nodes: HashMap<String, Node>,
    pub edges: HashMap<Node, Vec<Edge>>,
=======
    pub nodes: Vec<Node>,
    pub edges: Vec<Edge>,
>>>>>>> Stashed changes
}

/// A node within a gate
#[derive(Debug, Clone, Hash, PartialEq, Eq, Serialize, Deserialize)]
pub struct Node {
    pub type_: Type,
    pub name: String,
}

/// An edge on the graph
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Edge {
    /// (name of node, name of output on that node)
    pub source: (String, String),
    /// (name of node, name of input on that node)
    pub sink: (String, String),
<<<<<<< Updated upstream
=======
    pub state: Option<bool>,
>>>>>>> Stashed changes
}

#[derive(Debug, Clone, Hash, PartialEq, Eq, Serialize, Deserialize)]
pub enum Type {
    /// An input node - part of the public API of that gate
    Input,
    /// An output node - part of the public API of that gate
    Output,
    /// An internal node - not part of the public API of that gate. Contains within it the gate
    /// type
    Internal(String),
}
