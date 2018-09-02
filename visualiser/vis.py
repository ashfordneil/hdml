import json
import networkx as nx
import matplotlib.pyplot as plt
from graphviz import Source

f = open("out.json")

json_data = "".join(f.readlines())

python_data = json.loads(json_data)

# print(python_data)
circuit_graphs = []

for key in python_data:
    circuit = python_data[key]
    G = nx.MultiDiGraph(name=circuit)

    # Add nodes
    for node in circuit["nodes"]:
        node_details = circuit["nodes"][node]
        if node_details["type_"] == "Input" or node_details["type_"] == "Output":
            G.add_node(node, label=("<<TABLE><TR><TD>{}</TD></TR><TR><TD>{}</TD></TR></TABLE>>".format(node, node_details["type_"])))
        else:
            G.add_node(node, label=("<<TABLE><TR><TD>{}</TD></TR><TR><TD>{}</TD></TR></TABLE>>".format(node, node_details["type_"]["Internal"])))
    
    # Add edges
    for edge in circuit["edges"]:
        for outgoing in circuit["edges"][edge]:
            G.add_edge(outgoing["source"], outgoing["sink"][0])

    # Remove unused nodes
    to_remove = []
    for node in G.nodes():
        if nx.degree(G, node) == 0:
            to_remove.append(node)
    for node in to_remove:
        G.remove_node(node)

    circuit_graphs.append(G)

for i, graph in enumerate(circuit_graphs):
    dot = nx.nx_pydot.to_pydot(graph)
    src = Source(dot)
    src.render(str(i), view=True)
